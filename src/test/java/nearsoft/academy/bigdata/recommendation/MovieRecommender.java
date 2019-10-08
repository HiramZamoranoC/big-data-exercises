package nearsoft.academy.bigdata.recommendation;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;


public class MovieRecommender {

    private static final String OUTPUT_FILEPATH = "movies.csv";
    private static final String PRODUCT_KEY = "product/productId: ";
    private static final String USER_KEY = "review/userId: ";
    private static final String SCORE_KEY = "review/score: ";
    private int totalReviews;
    private HashMap<String, Integer> HMusers;
    private HashBiMap<String, Integer> HMproducts;
    private Recommender recommender;




  public MovieRecommender (String filePath) throws IOException, TasteException {
      this.HMusers = new HashMap<String, Integer>();
      this.HMproducts = HashBiMap.create();
      File csvFile = generateCSV(filePath);
      createRecommender(csvFile);
  }

  private File generateCSV(String filePath) throws IOException {

      BufferedReader reader = getGzipReader(new File(filePath));
      FileWriter writer = new FileWriter(OUTPUT_FILEPATH);
      String currentLine;
      String csvLine = "";
      int currentProduct = 0;

      while((currentLine = reader.readLine()) != null) {
          if (currentLine.startsWith(PRODUCT_KEY)) {
              String productId = currentLine.substring(19);
              if (!this.HMproducts.containsKey(productId)) {
                  this.HMproducts.put(productId, this.HMproducts.size());
              }
              currentProduct = this.HMproducts.get(productId);
          }

          else if (currentLine.startsWith(USER_KEY)) {
              String userId = currentLine.substring(15);
              if (!this.HMusers.containsKey(userId)){
                  this.HMusers.put(userId, HMusers.size());
              }
              this.totalReviews++;
              csvLine = this.HMusers.get(userId) + "," + currentProduct + ",";
          }

          else if (currentLine.startsWith(SCORE_KEY)) {
              double score = Double.parseDouble(currentLine.substring(14));
              csvLine += score + "\n";
              writer.write(csvLine);
              writer.flush();
          }



      }

      reader.close();
      writer.close();



      return new File(OUTPUT_FILEPATH);
  }



  public void createRecommender(File csvFile) throws TasteException, IOException {
      DataModel model = new FileDataModel(csvFile);
      UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
      UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
      this.recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
  }

  public List<String> getRecommendationsForUser (String userId) throws TasteException {
      int userIdInt = this.HMusers.get(userId);
      List recommendations = this.recommender.recommend(userIdInt, 3);
      return getRecommendationsIds(recommendations);
  }

private List<String> getRecommendationsIds(List<RecommendedItem> recommendations){
    BiMap<Integer, String> invertedProducts = this.HMproducts.inverse();
    ArrayList<String> productsIds = new ArrayList<String>();
     for (RecommendedItem recommendation : recommendations){
       int recommendationId = (int) recommendation.getItemID();
       productsIds.add(invertedProducts.get(recommendationId));
     }
     return  productsIds;
}




  public int getTotalReviews(){
      return totalReviews;
  }

  public int getTotalProducts(){
      return HMproducts.size();
  }

  public int getTotalUsers(){
      return HMusers.size();
  }



    private BufferedReader getGzipReader(File filePath) throws IOException {
        InputStream fileStream = new FileInputStream(filePath);
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, "UTF-8");

        return new BufferedReader(decoder);
    }



}
