package controllers

import dao.{ImageDAO, LabelDAO, LabelHasImageDAO}
import javax.inject._
import models.Puzzle
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, imageDAO: ImageDAO, labelDAO: LabelDAO, labelHasImageDAO: LabelHasImageDAO) extends AbstractController(cc) {

  val title = "Ultimate HEIG-VD Manager 2018"
  val numberOfImages = 15

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.ImageController.getImages,
        routes.javascript.HomeController.scorePuzzle
      )
    ).as("text/javascript")
  }

  def result = Action.async {
    val futurLabelHasImages = labelHasImageDAO.list()
    val futurLabels = labelDAO.list()
    val futurImages = imageDAO.list()

    for {
      labelHasImages <- futurLabelHasImages
      labels <- futurLabels
      images <- futurImages

      stats = for {
      labelHasImage <- labelHasImages
      label <- labels
      image <- images
      if (labelHasImage.imageId == image.id.get && labelHasImage.labelId == label.id.get)
    } yield (image.fileName,labelHasImage.imageId ,label.label, labelHasImage.clicks)

    } yield Ok(views.html.resultIndex("Projet Scala - Statistics", stats))
  }

  /**
    * Call the "about" html template.
    */
  def about = Action {
    Ok(views.html.about(title))
  }

  implicit val puzzleReads: Reads[Puzzle] = (
    (JsPath \ "clicked").read[Seq[String]] and
      (JsPath \ "notClicked").read[Seq[String]] and
      (JsPath \ "keyword").read[Long]
    ) (Puzzle.apply _)

  /**
    * Receives information on clicked and unclicked images, and a keyword.
    * Calculates the score and returns it to the user
    */
  def scorePuzzle = Action.async { implicit request =>
    val json: Puzzle = request.body.asJson.get.validate[Puzzle].get
    val keywordId: Long = json.keyword
    val clicked = json.clicked.map(_.toLong)
    val notClicked = json.notClicked.map(_.toLong)

    var falsePositive = 0
    var truePositive = 0
    var falseNegative = 0
    var trueNegative = 0

    for (id <- clicked) {
      for (image <- imageDAO.findById(id)) {
        image match {
          case Some(img) =>
            // The guy clicked, record it
            labelHasImageDAO.addAClick(id, keywordId)

            img.labelId match {
              // GG good label
              case Some(labelId) if labelId == keywordId =>
                truePositive += 1
              // The image has a label but not this one
              case Some(labelId) if labelId != keywordId =>
                falsePositive += 1
              // The image has no label, can't know if click correct or not
              case _ => // Do nothing
            }
          case _ => // The image doesn't exist, do nothing
        }
      }
    }

    for (id <- notClicked) {
      for (image <- imageDAO.findById(id)) {
        image match {
          case Some(img) =>
            img.labelId match {
              // GG you didn't click on this image
              case Some(labelId) if labelId != keywordId =>
                trueNegative += 1
              // Boy, you should have clicked
              case Some(labelId) if labelId == keywordId =>
                falseNegative += 1
              // The image has no label, can't know if click correct or not
              case _ => // Do nothing
            }
          case _ => // The image doesn't exist, do nothing
        }
      }
    }

    val images = imageDAO.findRandom(numberOfImages)
    val label = labelDAO.findRandom

    val score = truePositive + trueNegative - falsePositive - falseNegative

    for {
      images <- images
      label <- label
    } yield Ok(views.html.index("Your result has been recorded. Your score is " + score, images, label.get))
  }
}
