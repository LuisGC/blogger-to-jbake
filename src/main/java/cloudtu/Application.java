package cloudtu;

import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import cloudtu.blog.Post;
import cloudtu.blog.BloggerExtractor;
import cloudtu.blog.FileDownloader;
import cloudtu.blog.JbakeConverter;
import cloudtu.blog.SummaryReport;

public class Application {
	private static final Logger logger = Logger.getLogger(Application.class);
	
	private static final ResourceBundle rb = ResourceBundle.getBundle("application");	
	private static final String BLOG_ATOM_FILE_PATH = rb.getString("blogAtomFilePath");	
	private static final String OUTPUT_FOLDER_PATH;
	static{
		if(rb.getString("outputFolderPath").endsWith("/")){
			OUTPUT_FOLDER_PATH = StringUtils.substringBeforeLast(rb.getString("outputFolderPath"), "/");					
		}
		else{
			OUTPUT_FOLDER_PATH = rb.getString("outputFolderPath");
		}
	}	
	
	public static void main(String[] args) {
		logger.info("BloggerToJbake start");
		try {						
			List<Post> posts = new BloggerExtractor().extract(BLOG_ATOM_FILE_PATH);
			new JbakeConverter(posts).convertToFile(OUTPUT_FOLDER_PATH);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		finally{
			FileDownloader.shutdown();
		}
		logger.info(SummaryReport.getInstance());
		logger.info("BloggerToJbake stop");
	}
}
