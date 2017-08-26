/* Name:  

   UID:   

   Others With Whom I Discussed Things:

   Other Resources I Consulted: 
   
*/

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// a marker for code that you need to implement
class ImplementMe extends RuntimeException {}

// an RGB triple
class RGB {
    public int R, G, B;

    RGB(int r, int g, int b) {
    	R = r;
		G = g;
		B = b;
    }

    public String toString() { return "(" + R + "," + G + "," + B + ")"; }
}


// This class is used to compute mirror image by fork/join
class MirrorTask extends RecursiveTask< RGB[] > {
	private RGB[] originalPixels;
	private int low, // inclusive
				high; // exclusive
	private int width;
	private int SEQUENTIAL_CUTOFF;

    public MirrorTask(RGB[] a, int l, int h, int w) {
    	this.originalPixels = a;
    	this.low = l;
    	this.high = h;
    	this.width = w;
    	SEQUENTIAL_CUTOFF =  20 * width;
    	// width:       ~1.4 sec
    	// 2 * width:   ~1 sec
    	// 3 * width:   ~0.8 sec
    	// 4 * width:   ~0.7 sec
    	// 5 * width:   ~0.57 sec
    	// 6 * width:   ~0.6 sec
    	// 20 * width:  ~0.45 sec
    }

    public RGB[] compute() {
    	if(high - low > SEQUENTIAL_CUTOFF) { 
    		int mid = (low + high) / 2;
    		MirrorTask left = new MirrorTask(originalPixels, low, mid, width);
    		MirrorTask right = new MirrorTask(originalPixels, mid, high, width);

    		left.fork();
    		RGB[] rightMirror = right.compute();
    		RGB[] leftMirror = new RGB[originalPixels.length];
    		leftMirror = left.join();

    		for(int i = mid; i < high; i++) {
    			leftMirror[i] = rightMirror[i];
    		}

    		return leftMirror;
    	}
    	else {
    		RGB[] mirror = new RGB[originalPixels.length];

    		for(int i = low; i < high; i++) {
    			int col = i % width;
    			int theLastElementOfThisRow = (i / width + 1) * width - 1;
    			int mirrorIndex = theLastElementOfThisRow - col;
    			mirror[i] = new RGB(originalPixels[mirrorIndex].R, 
					    			originalPixels[mirrorIndex].G, 
					    			originalPixels[mirrorIndex].B );
    		}

    		return mirror;
    	}
    }
}
	
// This class is used to compute mirror image by fork/join
class GaussianTask extends RecursiveTask< RGB[] > {
	private RGB[] originalPixels;
	private int low, // inclusive
				high; // exclusive
	private int width, height;
	private int radius;
	private double sigma;
	//private final int SEQUENTIAL_CUTOFF = 10000;
	private int SEQUENTIAL_CUTOFF;

    public GaussianTask(RGB[] a, int l, int hi, int w, int he, int r, double s) {
    	this.originalPixels = a;
    	this.low = l;
    	this.high = hi;
    	this.width = w;
    	this.height = he;
    	this.radius = r;
    	this.sigma = s;
    	SEQUENTIAL_CUTOFF = 20 * width;
    	// (r = 20)
    	// 1 * width:    ~3.4 sec
    	// 2 * width:    ~3.2 sec
    	// 3 * width:    ~3.0 sec
    	// 5 * width:    ~2.8 sec
    	// 7 * width:    ~2.8 sec 
    	// 20 * width:   ~2.67 sec
    }

    
    private int clamp(int val, int min, int max) {
    	// min (inclusive)
    	// max (exclusive)
    	return Math.max(min, Math.min(max-1, val));
    }

    public RGB[] compute() {
    	if(high - low > SEQUENTIAL_CUTOFF) {
    		int mid = (low + high) / 2;
    		GaussianTask left = new GaussianTask(originalPixels, low, mid, width, height, radius, sigma);
    		GaussianTask right = new GaussianTask(originalPixels, mid, high, width, height, radius, sigma);

    		left.fork();
    		RGB[] rightGau = right.compute();
    		RGB[] leftGau = new RGB[originalPixels.length];
    		leftGau = left.join();

    		for(int i = mid; i < high; i++) {
    			leftGau[i] = rightGau[i];
    		}

    		return leftGau;

    	} else {
    		RGB[] gau = new RGB[originalPixels.length];

    		double[][] gaussianFilter = Gaussian.gaussianFilter(radius, sigma); // size: (2 * radius + 1) x (2 * radius + 1) 

    		for(int i = low; i < high; i++) {
    			int row = i / width;
    			int col = i % width;
    			int rowSize = 2 * radius + 1;
    			int colSize = 2 * radius + 1;
    			
    			int R = 0, G = 0, B = 0;
    			// get result r, g, b
    			for(int r = 0; r < rowSize; r++) {
    				for(int c = 0; c < colSize; c++) {
    					int rowClamped = clamp(row + (r - radius), 0, height);
    					int colClamped = clamp(col + (c - radius), 0, width);
    					int correspondingIndex = rowClamped * width + colClamped;
    					R += gaussianFilter[c][r] * originalPixels[correspondingIndex].R;
    					G += gaussianFilter[c][r] * originalPixels[correspondingIndex].G;
    					B += gaussianFilter[c][r] * originalPixels[correspondingIndex].B;
    				}
    			}
    			gau[i] = new RGB(R, G, B);
    		}
    		return gau;
    	}
    }
}

// an object representing a single PPM image
class PPMImage {
    protected int width, height, maxColorVal;
    protected RGB[] pixels;

    public PPMImage(int w, int h, int m, RGB[] p) {
		width = w;
		height = h;
		maxColorVal = m;
		pixels = p;
    }

    // parse a PPM image file named fname and produce a new PPMImage object
    public PPMImage(String fname) 
    	throws FileNotFoundException, IOException {
		FileInputStream is = new FileInputStream(fname);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		br.readLine(); // read the P6
		String[] dims = br.readLine().split(" "); // read width and height
		int width = Integer.parseInt(dims[0]);
		int height = Integer.parseInt(dims[1]);
		int max = Integer.parseInt(br.readLine()); // read max color value
		br.close();

		is = new FileInputStream(fname);
	    // skip the first three lines
		int newlines = 0;
		while (newlines < 3) {
	    	int b = is.read();
	    	if (b == 10)
				newlines++;
		}

		int MASK = 0xff;
		int numpixels = width * height;
		byte[] bytes = new byte[numpixels * 3];
        is.read(bytes);
		RGB[] pixels = new RGB[numpixels];
		for (int i = 0; i < numpixels; i++) {
	    	int offset = i * 3;
	    	pixels[i] = new RGB(bytes[offset] & MASK, 
	    						bytes[offset+1] & MASK, 
	    						bytes[offset+2] & MASK);
		}
		is.close();

		this.width = width;
		this.height = height;
		this.maxColorVal = max;
		this.pixels = pixels;
    }

	// write a PPMImage object to a file named fname
    public void toFile(String fname) throws IOException {
		FileOutputStream os = new FileOutputStream(fname);

		String header = "P6\n" + width + " " + height + "\n" 
						+ maxColorVal + "\n";
		os.write(header.getBytes());

		int numpixels = width * height;
		byte[] bytes = new byte[numpixels * 3];
		int i = 0;
		for (RGB rgb : pixels) {
	    	bytes[i] = (byte) rgb.R;
	    	bytes[i+1] = (byte) rgb.G;
	    	bytes[i+2] = (byte) rgb.B;
	    	i += 3;
		}
		os.write(bytes);
		os.close();
    }

	// implement using Java 8 Streams
    public PPMImage negate() {
		RGB[] negatePixels = new RGB[this.pixels.length];
		
		// Inititalize array negatePixels
		for(int i = 0; i < this.pixels.length; i++)
			negatePixels[i] = new RGB(this.pixels[i].R, this.pixels[i].G, this.pixels[i].B);

		Arrays.stream(negatePixels)
				.parallel()
				.forEach(rgb -> {
					rgb.R = this.maxColorVal - rgb.R;
					rgb.G = this.maxColorVal - rgb.G;
					rgb.B = this.maxColorVal - rgb.B;
				});

		return new PPMImage(width, height, maxColorVal, negatePixels);
    }

	// implement using Java 8 Streams
    public PPMImage greyscale() {
		RGB[] greyPixels = new RGB[this.pixels.length];
		
		// Initialize array greyPixels
		for(int i = 0; i < this.pixels.length; i++)
			greyPixels[i] = new RGB(this.pixels[i].R, this.pixels[i].G, this.pixels[i].B);

		Arrays.stream(greyPixels)
				.parallel()
				.forEach(rgb -> {
					rgb.R = (int) Math.round(.299 * rgb.R + .587 * rgb.G + .114 * rgb.B);
					rgb.G = rgb.R; 
					rgb.B = rgb.R;
				});

		return new PPMImage(width, height, maxColorVal, greyPixels);
    }    
    
	// implement using Java's Fork/Join library
    public PPMImage mirrorImage() {
		MirrorTask newPixels = new MirrorTask(pixels, 0, pixels.length, width);
		RGB[] mirrorPixels = newPixels.compute();
		return new PPMImage(width, height, maxColorVal, mirrorPixels);
    }

    // Convert RGB to integer
	// Example: 
	// Input: RGB = (255, 10, 3)
	// Output: 255010003
	private int toInteger(RGB rgb) { return (rgb.R * 1000000 + rgb.G * 1000 + rgb.B); }

    // Convert integer into RGB
	// Example:
	// Input: 255010003
	// Output: RGB = (255, 10, 3)
	private RGB toRGB(int inte) {
	    int r = inte / 1000000;
	    int g = (inte % 1000000) / 1000;
	    int b = (inte % 1000000) % 1000;
	    return new RGB(r, g, b);
	} 

	// implement using Java 8 Streams
    public PPMImage mirrorImage2() {
		int[] original = new int[pixels.length];
		int[] mirrorInt = new int[pixels.length];
		RGB[] mirrorRGB = new RGB[pixels.length];

		// Generate a stream of integers, one per pixel
		IntStream.range(0, pixels.length)
					.parallel()
					.forEach(i -> original[i] = toInteger(pixels[i])); 

		// Process these integers in parallel
		IntStream.range(0, pixels.length)
					.parallel()
					.forEach(i -> {
						int col = i % width;
						int theLastElementOfThisRow = (i / width + 1) * width - 1;
						int mirrorIndex = theLastElementOfThisRow - col;
						mirrorInt[i] = original[mirrorIndex];
					});

		// Convert integer array back to RGB array
		IntStream.range(0, pixels.length)
					.parallel()
					.forEach(i -> {
						mirrorRGB[i] = toRGB(mirrorInt[i]);
					});

		return new PPMImage(width, height, maxColorVal, mirrorRGB);
    }

	// implement using Java's Fork/Join library
    public PPMImage gaussianBlur(int radius, double sigma) {
		GaussianTask newPixels = new GaussianTask(pixels, 0, pixels.length, width, height, radius, sigma);
		RGB[] gaussianPixels = newPixels.compute();
		return new PPMImage(width, height, maxColorVal, gaussianPixels);
    }
}

// code for creating a Gaussian filter
class Gaussian {

    protected static double gaussian(int x, int mu, double sigma) {
		return Math.exp( -(Math.pow((x-mu)/sigma,2.0))/2.0 );
    }

    public static double[][] gaussianFilter(int radius, double sigma) {
		int length = 2 * radius + 1;
		double[] hkernel = new double[length];
		for(int i=0; i < length; i++)
	    	hkernel[i] = gaussian(i, radius, sigma);
		double[][] kernel2d = new double[length][length];
		double kernelsum = 0.0;
		for(int i=0; i < length; i++) {
	    	for(int j=0; j < length; j++) {
				double elem = hkernel[i] * hkernel[j];
				kernelsum += elem;
				kernel2d[i][j] = elem;
	    	}
		}
		for(int i=0; i < length; i++) {
	    	for(int j=0; j < length; j++)
				kernel2d[i][j] /= kernelsum;
		}
		return kernel2d;
    }
}


// Test
class Main {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		PPMImage image = new PPMImage("florence.ppm");
		PPMImage negateImage = image.negate();
		negateImage.toFile("florence_negate.ppm");

		PPMImage greyImage = image.greyscale();
		greyImage.toFile("florence_greyScale.ppm");

		PPMImage mirror = image.mirrorImage();
		mirror.toFile("florence_mirror.ppm");

		PPMImage mirror2 = image.mirrorImage2();
		mirror2.toFile("florence_mirror2.ppm");
	
		int r = 20;
		double sigma = 2.0;  
		PPMImage gaussian = image.gaussianBlur(r, sigma);
		gaussian.toFile("florence_gaussian.ppm"); 	
	}

}


