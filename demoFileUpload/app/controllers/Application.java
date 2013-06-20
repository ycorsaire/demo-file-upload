package controllers;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

import play.Play;
import play.Routes;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import views.html.index;

public class Application extends Controller {
  
    private static final String RANDOM_ID = "randomID";
	private static final String MIN_IMAGE_PREFIX = "min_";
	private static final String PUBLIC_UPLOAD_ROOT_DIR = "/public/upload/";

	public static Result index() {
		String randomID = java.util.UUID.randomUUID().toString();
        return ok(index.render(randomID));
    }
    
	public static Result upload() {
		MultipartFormData body = request().body().asMultipartFormData();
		if (body != null) {
			String uploadId = body.asFormUrlEncoded().get(RANDOM_ID)[0];
			FilePart mediaFile = body.getFile("files");
			File file = mediaFile.getFile();
			
			String baseDirPath = Play.application().path() + PUBLIC_UPLOAD_ROOT_DIR + uploadId;
			File dir = new File(baseDirPath);
			if(!dir.exists()) {
				dir.mkdir();
			}
			
			String filename = mediaFile.getFilename();
			String contentType = mediaFile.getContentType();

			try {
				// moved file to
				File movedFile = new File(dir, filename);
				FileUtils.moveFile(file, movedFile);
				
				//create and save miniature
				InputStream stream = new FileInputStream(movedFile);
				BufferedImage original = ImageIO.read(stream);
				Image miniature = original.getScaledInstance(160, 160*original.getHeight()/original.getWidth(), BufferedImage.SCALE_SMOOTH);
				File minFile = new File(dir, MIN_IMAGE_PREFIX + filename);
				String extension = contentType.substring(contentType.lastIndexOf("/") + 1);
				ImageIO.write(createBufImage(miniature), extension, minFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//return json result
			String url = routes.Application.download(uploadId + "/" + filename).url();
			String minurl = routes.Application.download(uploadId + "/" + MIN_IMAGE_PREFIX + filename).url();
			String deleteUrl =routes.Application.delete(uploadId,filename).url();
			ObjectNode buildImageJsonMap = buildMediaJsonMap(url, minurl, deleteUrl, filename, file.length(), contentType);
			ObjectNode files = Json.newObject();
			
			ArrayNode array = new ArrayNode(JsonNodeFactory.instance);
			
			array.add(buildImageJsonMap);
			
			files.put("files", array);
			return ok(files);
		}
		return ok("File uploaded");
	}
	
	private static ObjectNode buildMediaJsonMap(String url, String thumbnail_url, String deleteUrl, String fileName, long size, String contentType) {
		ObjectNode json = Json.newObject();
		json.put("url", url);
		json.put("thumbnail_url", thumbnail_url);
		json.put("name",fileName);
		json.put("type",contentType);
		json.put("size",size);
		json.put("delete_url", deleteUrl);
		json.put("delete_type","DELETE");
		return json;
	}
	
	private static BufferedImage createBufImage(Image image) {
		int width = image.getWidth(null);
		int height = image.getHeight(null);
		BufferedImage buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D graphics = buf.createGraphics();
		graphics.drawImage(image, 0, 0, null);
		graphics.dispose();
		return buf;
	}
	
	public static Result download(String filePath) {
		String path = Play.application().path() + PUBLIC_UPLOAD_ROOT_DIR + filePath;
		File file = new File(path);
		return ok(file);
	}
	
	public static Result delete(String uploadId, String filename) {
		String path = Play.application().path() + PUBLIC_UPLOAD_ROOT_DIR + uploadId + "/" + filename;
		String minpath = Play.application().path() + PUBLIC_UPLOAD_ROOT_DIR + uploadId + "/" + MIN_IMAGE_PREFIX + filename;
		
		
		File file = new File(path);
		file.delete();
		File minfile = new File(minpath);
		minfile.delete();
		
		return ok("OK");
	}
	
    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(
            Routes.javascriptRouter("jsRoutes",
                controllers.routes.javascript.Application.upload()
            )
        );
    }
  
}
