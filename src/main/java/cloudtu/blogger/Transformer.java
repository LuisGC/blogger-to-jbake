package cloudtu.blogger;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Transformer {
	private static final Logger logger = Logger.getLogger(Transformer.class);

	private static final int CONCURRENT_THREAD_AMOUNT = 100;
	
	private List<Article> inputArticles;
	private String outputFolderPath;
	
	public Transformer(List<Article> inputArticles, String outputFolderPath) {
		this.inputArticles = inputArticles;
		this.outputFolderPath = outputFolderPath;
	}
	
	public void traslateToJbakeFormatHtmlFile() throws RuntimeException{
		ThreadPoolExecutor executor = null;
		try {
			executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(CONCURRENT_THREAD_AMOUNT);
			
			// 先把舊資料清掉
			if(new File(outputFolderPath).exists()){
				FileUtils.deleteDirectory(new File(outputFolderPath));				
			}			
			
			for (Article article : inputArticles) {
				final String year = DateFormatUtils.format(article.getDate(), "yyyy");
				final String month = DateFormatUtils.format(article.getDate(), "MM");
				String imgFileNamePrefix = DateFormatUtils.format(article.getDate(), "yyyyMMddHHmm") + "_";
				
				Document doc = Jsoup.parseBodyFragment(article.getContent());			
				
				// 處理 <img> tag
				Elements imgs = doc.select("img");
				for (Element img : imgs) {
					final String imgSrcUrl = img.attr("src");
					final String imgFileName = imgFileNamePrefix + StringUtils.substringAfterLast(imgSrcUrl, "/");
					
					// 把 <img> 裡的圖片檔下載到 local 端
					executor.submit(new Runnable() {						
						@Override
						public void run() {											
							try {
								logger.info("save image : " + imgSrcUrl + " to " + String.format("%s/img/%s/%s/%s", outputFolderPath, year, month, imgFileName));
								FileUtils.copyURLToFile(new URL(imgSrcUrl), 
										new File(String.format("%s/img/%s/%s/%s", outputFolderPath, year, month, imgFileName)),
										1000,3000);
							}
							catch (Exception ex) {
								logger.error(ex.getMessage());
							}							
						}
					});
					
					// 修改 <img src="{imgFileName}">，讓它變成   <img src="../../../img/{year}/{month}/{imgFileName}">
					img.attr("src", String.format("../../../img/%s/%s/%s", year, month, imgFileName));
					
					// 如果 <img src="{imgFilePath}"> 外層包了 <a>，修改  <a href="{imgFilePath}"> 內容		
					if(img.parent().tagName().equalsIgnoreCase("a")){
						img.parent().attr("href", String.format("../../../img/%s/%s/%s", year, month, imgFileName));
					}
				}
				
				// 處理 <pre class="brush: xxx"> tag，讓它變成 <pre class="prettyprint"><code></code></pre>
				Elements pres = doc.select("pre[class^=brush:]");
				for (Element pre : pres) {
					pre.attr("class", "prettyprint");
					pre.html("<code>" + pre.html() + "</code>");
				}
				

				article.setContent(doc.body().html());
				
				StringBuilder jbakeFormatHtmlContent = new StringBuilder();
				jbakeFormatHtmlContent.append("title=").append(article.getTitle()).append("\n")
								.append("date=").append(DateFormatUtils.format(article.getDate(), "yyyy-MM-dd")).append("\n")
								.append("type=post\n")
								.append("tags=").append(article.getTagsAsString()).append("\n")
								.append("status=published\n")
								.append("~~~~~~\n\n")
								.append(article.getContent());
				logger.info("save html : " + String.format("%s/blog/%s/%s/%s", outputFolderPath, year, month, article.getFileName()));
				FileUtils.writeStringToFile(new File(String.format("%s/blog/%s/%s/%s", outputFolderPath, year, month, article.getFileName())), 
								jbakeFormatHtmlContent.toString(), Charset.forName("UTF-8"));
			}
			
			boolean isAllImgSaved = false;
			while(true){
				isAllImgSaved = (executor.getQueue().isEmpty() & (executor.getActiveCount() == 0));
				if(isAllImgSaved){
					break;
				}
				
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					logger.warn(e.getMessage());
				} 				
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally{
			try {
				if(executor != null){
					executor.shutdown();					
				}
			}
			catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}			
	}	
}