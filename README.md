# Parallel Image Processing

Programming language: Java

CS 131, Spring 2016

Processing large images can quickly become computation-intensive. Fortunately, many image transformations are naturally parallelizable, since each pixel (or groups of a small number of pixels) can often be processed independently. This parallel image processing can process image in parallel. The features includes: negate, greyscale, mirror image, and Gaussian blur

### To process the image:
 1. Clone this repo
 ```
 git clone https://github.com/appletreeisyellow/Parallel-Image-Processing.git
 ```
 2. Go to the directory of this project
 ```
 cd Parallel-Image-Processing
 ```
 3. Compile 
 ```
 javac hw5.java
 ```
 4. Run 
 ```
 java Main
 ```
 5. Now you can see the processed images
 
 


### How to open PPM Images

Some image viewing applications can directly display ppm files, including the Aquamacs text editor on Mac OS X. However, many image-viewing applications require that the image first be converted to a JPEG. This can be done with the pnmtojpeg program at the command line as follows: pnmtojpeg input-file.ppm > output-file.jpg. This program comes with many Linux distributions as well as with Cygwin for Windows. If you don't already have it, you can download pnmtojpeg as part of the NetPBM library. (The library also includes the program jpegtopnm for converting JPEGs into the ppm format; you can use this program to convert your favorite images so they can be manipulated by your code.)
