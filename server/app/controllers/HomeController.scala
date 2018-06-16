package controllers

import dao.ImageDAO
import javax.inject._
import models.Image
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter

import scala.concurrent.ExecutionContext.Implicits.global


/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, imageDAO: ImageDAO) extends AbstractController(cc) {

  val title = "Ultimate HEIG-VD Manager 2018"

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        //        routes.javascript.StudentsController.getStudents,
        routes.javascript.ImageController.getImages,
        routes.javascript.HomeController.postImage
      )
    ).as("text/javascript")
  }

  def index = Action.async {
    val images = imageDAO.list()
    images map { images =>
      Ok(views.html.index("Salut copain", images))
    }
  }

  // Declare a case class that will be used in the new image's form
  case class ImageRequest(fileName: String)

  // Need to import "play.api.data._" and "play.api.data.Forms._"
  def imageForm = Form(
    mapping(
      "fileName" -> text,
    )(ImageRequest.apply)(ImageRequest.unapply)
  )

  /**
    * Called when the user try to post a new student from the view.
    * See https://scalaplayschool.wordpress.com/2014/08/14/lesson-4-handling-form-data-with-play-forms/ for more information
    * Be careful: if you have a "Unauthorized" error when accessing this action you have to add a "nocsrf" modifier tag
    * in the routes file above this route (see the routes file of this application for an example).
    */
  def postImage = Action.async { implicit request =>
    val referer = request.headers.get("referer")
    val imageRequest = imageForm.bindFromRequest.get
    val newImage = Image(null, imageRequest.fileName, null)

    imageDAO.insert(newImage) map (_ =>
      Redirect(routes.HomeController.index())
      )
  }

  /**
    * Call the "about" html template.
    */
  def about = Action {
    Ok(views.html.about(title))
  }
}
