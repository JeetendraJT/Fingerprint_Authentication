package root;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.digitalpersona.uareu.Engine;
import com.digitalpersona.uareu.Fid;
import com.digitalpersona.uareu.Fmd;
import com.digitalpersona.uareu.Importer;
import com.digitalpersona.uareu.UareUException;
import com.digitalpersona.uareu.UareUGlobal;

/**
 *
 * INSTRUCTIONS FOR RUNNING TEST BY TA:
 * Create FMDs list and iterate it one by one as done at line number : 110
 * Look for string "-----------INSERTION POINT FOR TEST BY TA---------------"
 *
 */ 
public class FingerPrintVerificationUareU {
	
	public static void main(String[] args) {

		try {
			 Engine eng = UareUGlobal.GetEngine();
			 
			 // All folders for each student 
			 // test is a folder where we have put test data
			 String[] students = {"Person1", "Person2", "Person3", "Person4", "Person5"
					 ,"Person6", "Person7", "Person8", "Person9", "Person10", "Test"};
			
			 //A map to hold all FMDs of all students - Key is Student and Value is List of FMDs
			 Map<String, List<Fmd>> studentRecord = new LinkedHashMap<String, List<Fmd>>();
			 
			 for(String student:students){
				// for each student
				File dir = new File("D:\\My notes\\USF College\\Spring 2017\\" +
			 		"Biometrics\\Projects\\p2\\All\\"+student);
				File[] directoryListing = dir.listFiles();
				 
				// create byte array for all images to be used in creating FIDs
				BufferedImage image = null;
				List<byte[]> byteArrayList = new ArrayList<byte[]>();
				for(File eachImage : directoryListing){
					image = ImageIO.read(new File(eachImage.getAbsolutePath()));
					// get DataBufferBytes from Raster
					WritableRaster raster = image.getRaster();
					DataBufferByte data = (DataBufferByte) raster.getDataBuffer();
					byteArrayList.add(data.getData());
					image.flush();
				}

				//create fiDs from all image's byte[]
				Importer importer = UareUGlobal.GetImporter(); 
				List<Fid> fIDList = new ArrayList<Fid>();
				Fid fid = null;
				for(byte[] eachImageByteArray : byteArrayList){
					/**
					 * Fid ImportRaw(byte[] data, int width, int height,int dpi,int finger_position,
						             int cbeff_id,Fid.Format out_format, int out_dpi,boolean rotate180)
					 */
					fid = importer.ImportRaw(eachImageByteArray, 320, 480, 500, 1, 1,
							Fid.Format.ISO_19794_4_2005, 500, false);
					// for 300 dpi, it gives error so use 500(recommended for most of the algorithms)
					fIDList.add(fid);
				}
				
				//create FMDs from all FIDs
				List<Fmd> fMDList = new ArrayList<Fmd>();
				Fmd fMD = null;
				for(Fid eachImageFID : fIDList){
					/**
					 * Fmd CreateFmd(Fid fid,  Fmd.Format format)
					 */
					fMD = eng.CreateFmd(eachImageFID, Fmd.Format.ISO_19794_2_2005);
					fMDList.add(fMD);
				}
				// Add list of FMDs to a map for each student
				studentRecord.put(student,fMDList);
				 
			 }
			
			/*
			 * studentRecord contains fmVs of test data also so I will separate out it 
			 * wrt key "Test" which was a folder name for test data. 
			 */
			List<Fmd> testDataFMDs = studentRecord.get("Test");
			
			// Now remove Test data from student records
			studentRecord.remove("Test");
			 
			//User defined threshold - can be changed as done to find ROC.
			int thresholdScore = (int) (0.0005*Engine.PROBABILITY_ONE);
			
			//-----------INSERTION POINT FOR TEST BY TA---------------
			for(Fmd currentTestFmd: testDataFMDs)
			{
				Map<String, Integer> testDataResultMap = new LinkedHashMap<String, Integer>(); 
				// for each test data, we are maintaining above map
				
				for(Map.Entry<String, List<Fmd> > entry : studentRecord.entrySet())
				{
					String currStudentName = entry.getKey(); // student name
					List<Fmd> currStudentFMDs = entry.getValue(); // list of FMDs recorded for this student
					
					int minMatchScore = 2147483647;
					
					for(Fmd storedFMD: currStudentFMDs)
					{
						int matchScore = eng.Compare(currentTestFmd, 0, storedFMD, 0);
						if(matchScore < minMatchScore)
						{
							minMatchScore = matchScore;// store min score
						}
					}
					if(minMatchScore < thresholdScore)
					{
						testDataResultMap.put(currStudentName, minMatchScore);
					}
				}
				// Now , we have Map
				// Find minimum match value and corresponding student
				int tempMatchValue = 2147483647; // given maximum for sake of simplicity
				String studentName = "";
				String allMatchedStudents = "";
				for(String key: testDataResultMap.keySet())
				{
					int matchScore = testDataResultMap.get(key);
					allMatchedStudents = allMatchedStudents + ", " + key; // for false positive
					if(matchScore < tempMatchValue)
					{ // find minimum
						tempMatchValue = matchScore;
						studentName = key;
					}
				}
				System.out.println("This test image got matched with "+studentName + " with minimum " +
						"match score as "+tempMatchValue + " " +
								"and all students matched are : " + allMatchedStudents);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UareUException e) {
			e.printStackTrace();
		}
		
	}
}
