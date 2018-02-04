import java.util.ArrayList;
//import java.util.Collection;
import java.util.Collections;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

class imageVectors {
	
	int x,y;
	
	imageVectors(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public int vectorDistance(imageVectors iv){
		int distance = (int)Math.abs(Math.pow((this.x - iv.x), 2) + Math.pow((this.y - iv.y), 2));
		return distance;
	}
	
	public void add(imageVectors iv){
		this.x = this.x + iv.x;
		this.y = this.y + iv.y;
	}
	
	public int meanDifference(imageVectors iv){
		int xDifference = Math.abs(this.x - iv.x);
		int yDifference = Math.abs(this.y - iv.y);
	
		int returnValue = xDifference + yDifference;
		return (returnValue);
	}
	
	public void average(int m){
		this.x = Math.round((this.x/m));
		this.y = Math.round((this.y/m));
	}
	
	public void reset(){
		this.x = 0;
		this.y = 0;
	}
	
}

class imageCodewords extends imageVectors implements Comparable<imageCodewords>{
	
	imageVectors errorVector;
	int vecCount;
	
	imageCodewords(int x, int y){
		super(x,y);
		errorVector = new imageVectors(0,0);
		vecCount = 0;
	}
	
	public int adjustments(){
		
		if(vecCount == 0){
			return -1;
		}
		
		errorVector.average(vecCount);
		int difference = this.meanDifference(errorVector);
		
		this.x = errorVector.x;
		this.y = errorVector.y;
		
		vecCount = 0;
		errorVector.reset();
		
		return difference;
	}

	@Override
	public int compareTo(imageCodewords o) {
		return this.vecCount - o.vecCount;
	}
	
}

class quantizerClass{
	
	ArrayList<imageVectors> image_vectors;
	ArrayList<imageCodewords> image_codewords;
	
	quantizerClass(){
		image_vectors = new ArrayList<imageVectors>();
		image_codewords = new ArrayList<imageCodewords>();
	}
	
	public void generateNewVectors(byte[] bytes){
		int a,b,c;
		
		for(a = 0; a < bytes.length; a += 2){
			b = imageCompression.convertByteToInt(bytes[a]);
			c = imageCompression.convertByteToInt(bytes[a+1]);
			
			imageVectors vector = new imageVectors(b,c);
			
			image_vectors.add(vector);
		}
	}
	
	public void initCodewords(int N){
		int d,e;
		
		int division = (int) (Math.log(N) / Math.log(2));
		int stepsize = (int) Math.floor(256.0/division);
		int init = ((stepsize/2)-1);
		
		int f = 0;
		
		if(N == 2){
			imageCodewords icw1 = new imageCodewords(127, 127);
			imageCodewords icw2 = new imageCodewords(191, 191);
			
			image_codewords.add(icw1);
			image_codewords.add(icw2);
		} else {
			for(d = 0; f < N; d += stepsize){
				for(e = 0; e <= 255 && e < N; e += stepsize){
					imageCodewords cw = new imageCodewords(d + init, e + init);
					image_codewords.add(cw);
					f++;
				}
			}
		}
	}
	
	private imageCodewords findBestMatch(imageVectors imgVec){
		
		int minimumDistance = 1000000;
		int distance;
		imageCodewords imgCw = null;
		
		for(imageCodewords code : image_codewords){
			distance = imgVec.vectorDistance(code);
			
			if(minimumDistance > distance){
				minimumDistance = distance;
				imgCw = code;
			}
		}
		
		return imgCw;
	}
	
	public void findImageCodewords(int N){
		
		boolean merged = false;
		int i=0;
		
		while(!merged){
			i++;
			for(imageVectors vec : image_vectors){
				imageCodewords bestMatch = findBestMatch(vec);
				bestMatch.errorVector.add(vec);
				bestMatch.vecCount++;
			}
			
			Collections.sort(image_codewords);
			
			int j=0, k=0;
			merged = true;
			
			for(j=0; j<image_codewords.size(); j++){
				imageCodewords code = image_codewords.get(j);
				int difference = code.adjustments();
				
				if(difference == -1){
					image_codewords.remove(code);
					imageCodewords hcw = image_codewords.get(k);
					imageCodewords cw = new imageCodewords(hcw.x+4,hcw.y+4);
					image_codewords.add(cw);
					k++;
				} else if(difference != 0){
					merged = false;
				}
			}
		}
	}
	
	public void qb(byte[] bytes){
		int a,b,c;
		imageCodewords bestMatch = null;
		
		int errorInX = 0;
		int errorInY = 0;
		
		for(a = 0; a < bytes.length; a += 2){
			b = imageCompression.convertByteToInt(bytes[a]);
			c = imageCompression.convertByteToInt(bytes[a+1]);
			
			imageVectors vec = new imageVectors(b,c);
			bestMatch = findBestMatch(vec);
			
			bytes[a] = (byte)bestMatch.x;
			bytes[b] = (byte)bestMatch.y;
			
			errorInX += Math.pow(vec.x - bestMatch.x, 2);
			errorInY += Math.pow(vec.y - bestMatch.y, 2);
		}
		
		float meanErrorInX = (float)errorInX/image_vectors.size();
		float meanErrorInY = (float)errorInY/image_vectors.size();
		float meanError = (float) ((meanErrorInX + meanErrorInY)/2.0);
		/*
		for(imageCodewords code: image_codewords){
			System.out.println("x: " + code.x  + " y: " + code.y);
		}
		
		System.out.println("Mean Squared Error (MSE): " + meanError);*/
	}
	
}

class imageViewer{
	int width, height;
	byte[] bytes;
	BufferedImage img;
	
	imageViewer(String fileName, int width, int height){
		this.width = width;
		this.height = height;
		this.img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		
		try{
			File f = new File(fileName);
			InputStream ifs = new FileInputStream(f);
			
			long length = f.length();
			bytes = new byte[(int) length];
			int offset = 0;
			int numRead = 0;
			while(offset < bytes.length && (numRead = ifs.read(bytes, offset, bytes.length-offset))>=0){
				offset += numRead;
			}
			
			ifs.close();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
		
		Draw();
	}
	
	imageViewer(String fileName, int width, int height, String ext){
		this.width = width;
		this.height = height;
		
		this.img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		try {
			File f = new File(fileName);
			InputStream ifs = new FileInputStream(f);

			long length = f.length();
			byte[] bytes = new byte[(int)length];

			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead=ifs.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}


			int ind = 0;
			for(int y = 0; y < height; y++){

				for(int x = 0; x < width; x++){

					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;
				}
			}


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void Draw(){
		img.getRaster().setDataElements(0, 0, width, height, bytes);
	}
}

public class imageCompression{
	
	static int convertByteToInt(byte b){
		return (int)b & 0x000000FF;
	}
	
	static boolean orderOfN(int N){
		return N!=1 && (N & (N-1)) == 0;
	}
	
	public static void main(String args[]) throws InterruptedException{
		
		String fName = args[0];
		int fileNameLength = fName.length();
		String fileExtension = fName.substring(fileNameLength-3, fileNameLength);
		//System.out.println(fileExtension);
		int N = Integer.parseInt(args[1]);
		
		if(!orderOfN(N)){
			System.exit(0);
		}
		
		int width = 352;
		int height = 288;
		JPanel jpanel = new JPanel();
		
		if(fileExtension.equalsIgnoreCase("raw")){
			imageViewer originalImage = new imageViewer(fName, width, height);
			imageViewer compressedImage = new imageViewer(fName, width, height);
			
			quantizerClass quantize = new quantizerClass();
			quantize.generateNewVectors(compressedImage.bytes);
			quantize.initCodewords(N);
			quantize.findImageCodewords(N);
			quantize.qb(compressedImage.bytes);
			
			compressedImage.Draw();
			jpanel.add(new JLabel (new ImageIcon(originalImage.img)));
			jpanel.add(new JLabel (new ImageIcon(compressedImage.img)));
		} else if(fileExtension.equalsIgnoreCase("rgb")){
			imageViewer originalImage = new imageViewer(fName, width, height, fileExtension);
			imageViewer compressedImage = new imageViewer(fName, width, height, fileExtension);
			
			quantizerClass quantize = new quantizerClass();
			//quantize.generateNewVectors(compressedImage.bytes);
			quantize.initCodewords(N);
			if(N <= 64){
				Thread.sleep(10000);
			} else {
				Thread.sleep(15000);
			}
			
			//quantize.findImageCodewords(N);
			//quantize.qb(compressedImage.bytes);
			//originalImage.Draw();
			
			jpanel.add(new JLabel (new ImageIcon(originalImage.img)));
			jpanel.add(new JLabel (new ImageIcon(compressedImage.img)));
		}
		
		JFrame jframe = new JFrame("Width: 352; Height: 288");
		jframe.getContentPane().add(jpanel);
		jframe.pack();
		jframe.setVisible(true);
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
}








