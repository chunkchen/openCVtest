package xyz.viseator;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import static org.opencv.imgproc.Imgproc.*;

/**
 * Wudi
 * viseator@gmail.com
 * Created by viseator on 2016/12/12.
 */

public class CutPic {
    private static final int BLOCK_SIZE = 27;
    private static final int C_THRESHOLD = 10;

    //the minimum length of line when find the horizontal lines of table
    private static final double Y_MINLINELENGTH_FACTOR = 0.75;
    //the threshold when find the horizontal lines of table
    private static final int Y_THRESHOLD = 150;
    //the max gap of the intermittent line
    private static final double Y_MAXLINEGAP_FACTOR = 0.01;

    //similar with above,using in find vertical lines
    private static final int X_THRESHOLD = 0;
    //similar with above,using in find vertical lines
    private static final double X_MAXLINEGAP_FACTOR = 0.15;
    //factor for the minimum length of line,X_MINLINELENGTH = image.height() * X_HEIGHT_FACTOR
    private static final double X_HEIGHT_FACTOR = 0.8;

    //the scale of a character's width in picture's width
    private static final double CHARACTER_SIZE = 0.019;
    private static final double IMAGE_WIDTH = 2592.0;
    private static final double TABLE_CHARACTER_FACTOR = 0.43;
    private static final double FILTER_GAP = 0.1;

    private ArrayList<Mat> blockImages; //Store rows
    private ArrayList<RowInfo> rows;

    private Mat srcPic;
    private Mat dilateMuchPic;
    private int picId;
    private double characterWidth;
    private int colNum = -1; //when find a valid col:colNum++

    private RecognizeCharacters ocr;
    private IndexReader indexReader;

    public CutPic() {
        indexReader = new IndexReader("./index.txt");
    }

    public ArrayList<RowInfo> progress(String path, int picId) {
        srcPic = Imgcodecs.imread(path, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        this.picId = picId;

        rows = new ArrayList<>();
        binarization();
        deNoise();
        cutImagesToRows();
        cutImagesToCols();
        getContentOfRow();
        return rows;
    }

    public void setOcr(RecognizeCharacters ocr) {
        this.ocr = ocr;
    }

    /**
     * binarization the srouce picture
     */
    private void binarization() {
        //blockSize and C are the best parameters for table
        Imgproc.adaptiveThreshold(srcPic, srcPic, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY_INV, BLOCK_SIZE, C_THRESHOLD);
        Imgcodecs.imwrite("C:/Users/visea/Desktop/test/new/binarization/" + String.valueOf(picId) + ".jpg", srcPic);
    }

    /**
     * get the indexes in index.txt to get the right answer
     *
     * @return Set<String> indexes
     */
    public Set<String> getIndexes() {
        return indexReader.getName();
    }

    /**
     * remove the isolated point in the picture
     * erode the picture to remove
     * then dilate it to recover others
     */
    private void deNoise() {
        //kernel for erode, size:the size of the erosion
        Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(2, 2));
        Imgproc.erode(srcPic, srcPic, kernelErode);

        //kernel for dilate, size:the size off the dilation
        Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(2, 2));
        Imgproc.dilate(srcPic, srcPic, kernelDilate);

        //dilate much, for finding all lines
        dilateMuchPic = new Mat();
        Mat kernelDilateMuch = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(12, 12));
        Imgproc.dilate(srcPic, dilateMuchPic, kernelDilateMuch);

        Imgcodecs.imwrite("C:/Users/visea/Desktop/test/new/deNoise/" + String.valueOf(picId) + ".jpg", srcPic);
    }

    /**
     * cut images to rows and store in blockImages
     */
    private void cutImagesToRows() {
        ArrayList<Double> lineYs = new ArrayList<>();
        ArrayList<Double> uniqueLineYs = new ArrayList<>();

        //lines:a special mat for find lines
        Mat lines = new Mat();
        //find lines and store in lines
        Imgproc.HoughLinesP(dilateMuchPic, lines, 1, Math.PI / 180, Y_THRESHOLD,
                Y_MINLINELENGTH_FACTOR * srcPic.width(), srcPic.width() * Y_MAXLINEGAP_FACTOR);

        //get the lines information from lines and store in lineYs
        for (int i = 0; i < lines.rows(); i++) {
            double[] points = lines.get(i, 0);
            double y1, y2;

            //just need the horizontal lines
            y1 = points[1];
            y2 = points[3];

            // if it slopes, get the average of them, store the y-coordinate
            if (Math.abs(y1 - y2) < 100) {
                lineYs.add((y1 + y2) / 2);
            }
        }

        getUniqueLines(lineYs, uniqueLineYs, 10);

        srcPic = new Mat(srcPic, new Rect(0, uniqueLineYs.get(0).intValue(), srcPic.width(),
                (int) ((uniqueLineYs.get(uniqueLineYs.size() - 1) - uniqueLineYs.get(0)))));

        for (int index = 1; index < uniqueLineYs.size(); index++) {
            uniqueLineYs.set(index, uniqueLineYs.get(index) - uniqueLineYs.get(0));
        }

        uniqueLineYs.remove(uniqueLineYs.size() - 1);
        uniqueLineYs.remove(0);
        showLines(srcPic, uniqueLineYs, false);

        blockImages = new ArrayList<>();
        if (uniqueLineYs.size() == 0) blockImages.add(srcPic);

        for (int i = 0; i < uniqueLineYs.size(); i++) {
            Rect rect;
            double y = uniqueLineYs.get(i);
            if (i == 0) {
                rect = new Rect(0, 0, srcPic.width(), (int) y);
                blockImages.add(new Mat(srcPic, rect));
            }
            if (i != uniqueLineYs.size() - 1) {
                rect = new Rect(0,
                        (int) (y),
                        srcPic.width(),
                        (int) (uniqueLineYs.get(i + 1) - y));
            } else {
                //the last line
                rect = new Rect(0,
                        (int) (y),
                        srcPic.width(),
                        (int) (srcPic.height() - y));
            }
            //cut the source picture to cutMat
            Mat cutMat = new Mat(srcPic, rect);

            Imgcodecs.imwrite("C:/Users/visea/Desktop/test/new/temp/Rows" +
                            String.valueOf(picId) + String.valueOf(++colNum) + ".jpg"
                    , cutMat);
            blockImages.add(cutMat);
        }
    }


    /**
     * cut the rows in blockImages to cols
     */
    private void cutImagesToCols() {
        for (int position = 0; position < blockImages.size(); position++) {
            Mat image = blockImages.get(position);

            //dilate much to find all lines
            Mat kernelDilateMuch = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(6, 6));
            Imgproc.dilate(image, dilateMuchPic, kernelDilateMuch);

            //find lines
            Mat lines = new Mat();
            Imgproc.HoughLinesP(dilateMuchPic, lines, 0.01, Math.PI / 360, X_THRESHOLD,
                    image.height() * X_HEIGHT_FACTOR, image.height() * X_MAXLINEGAP_FACTOR);

            ArrayList<Double> lineXs = new ArrayList<>();
            for (int i = 0; i < lines.rows(); i++) {
                double[] points = lines.get(i, 0);
                double x1, x2;

                x1 = points[0];
                x2 = points[2];

                if (Math.abs(x1 - x2) < 50) {
                    lineXs.add((((x1 + x2) / 2)));//store the x-coordinate
                }
            }
            ArrayList<Double> uniqueLineXs = new ArrayList<>();

            getUniqueLines(lineXs, uniqueLineXs, 10);
//            showLines(image, uniqueLineXs, true);
            //filter the invalid lines
//            ArrayList<Double> betterLineXs = new ArrayList<>();
//            filterLines(uniqueLineXs, betterLineXs, image.width());

            //filter the image that have too much or too less lines or too small height
//            if (betterLineXs.size() < 2 || betterLineXs.size() > 5 || image.height() < srcPic.height() * 0.015) {
//                continue;
//            }

            //find a valid image
//            showLines(image, uniqueLineXs, true);
            String nameOfRow = null;
            try {
                nameOfRow = getNameOfRow(uniqueLineXs, image);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                continue;
            }

            ArrayList<Double> filteredLineXs = new ArrayList<>();
            filterLines(uniqueLineXs, filteredLineXs);
            showLines(image, filteredLineXs, true);
            RowInfo rowInfo = indexReader.getRowInfo(nameOfRow);

            int leftBorder = rowInfo.getLeftBorder();
            int rightBorder = rowInfo.getRightBorder();

            Rect rect;

            if (filteredLineXs.size() > rightBorder) {
                rect = new Rect(filteredLineXs.get(leftBorder).intValue() + 5,
                        0,
                        (int) (filteredLineXs.get(rightBorder) - filteredLineXs.get(leftBorder) - 10), image.height());
            } else {
                rect = new Rect(filteredLineXs.get(leftBorder).intValue() + 5,
                        0,
                        (int) (image.width() - filteredLineXs.get(leftBorder) - 10), image.height());
            }
            Mat contentImage = new Mat(image, rect);
            rowInfo.setContentImage(contentImage);
            rows.add(rowInfo);
        }
        for (RowInfo rowInfo : rows) {
            if (rowInfo != null) {
                Imgcodecs.imwrite("C:/Users/visea/Desktop/test/new/contentImages/" +
                                String.valueOf(picId) + String.valueOf(++colNum) + ".jpg"
                        , rowInfo.getContentImage());
            }
        }
    }


    private void getContentOfRow() {
        for (RowInfo rowInfo : rows) {
            Mat contentImage = rowInfo.getContentImage();
            try {

                ArrayList<Mat> characters = cutCharacters(contentImage, rowInfo.getDataType(), false);

                for (Mat character : characters) {
                    Imgcodecs.imwrite("C:/Users/visea/Desktop/test/new/content/" +
                                    String.valueOf(picId) + String.valueOf(++colNum) + ".jpg"
                            , character);
                }
                rowInfo.setResult(ocr.recognize(convertMatsToBufferedImages(characters), rowInfo.getDataType(), false));
                System.out.println(ocr.recognize(convertMatsToBufferedImages(characters), rowInfo.getDataType(), false));
            }catch (Exception e){
                System.out.println("Error when get content of row");
            }
        }
    }


    private String getNameOfRow(ArrayList<Double> coordinates, Mat mat) {
        Mat cutMat = new Mat(mat, new Rect(coordinates.get(0).intValue() + 10, 5
                ,
                (int) (coordinates.get(1) - coordinates.get(0) - 30), mat.height() - 10));

        ArrayList<Mat> characters = cutCharacters(cutMat, RowInfo.IS_STRING, true);

        for (Mat character : characters) {
            Imgcodecs.imwrite("C:/Users/visea/Desktop/test/new/nameOfRow/" +
                            String.valueOf(picId) + String.valueOf(++colNum) + ".jpg"
                    , character);
        }
        System.out.println(ocr.recognize(convertMatsToBufferedImages(characters), RowInfo.IS_STRING, true));

        return ocr.recognize(convertMatsToBufferedImages(characters), RowInfo.IS_STRING, true);
    }

    private ArrayList<Mat> cutCharacters(Mat mat, int dataType, boolean isIndex) {
        ArrayList<ArrayList<Mat>> singleLines = new ArrayList<>();
        //store the y-coordinates of empty rows (which has few white pixel)
        ArrayList<Double> emptyRows = new ArrayList<>();
        ArrayList<Double> uniqueEmptyRows = new ArrayList<>();

        //walking all of pixels of each rows, when the count of white pixels less than 3, store the y-coordinate of row
        double[] points;
        for (int row = 0; row < mat.rows(); row++) {
            int count = 0;
            for (int col = 10; col < mat.cols(); col++) {
                points = mat.get(row, col);
                if (points[0] == 255) {
                    count++;
                }
            }
            if (count < 2) emptyRows.add((double) row);
        }

        if (isIndex) {
            getBorders(emptyRows, uniqueEmptyRows, 5, 0);
            showLines(mat, uniqueEmptyRows, false);
            Mat cutMat = new Mat(mat, new Rect(0,
                    (uniqueEmptyRows.get(1).intValue()),
                    mat.width(),
                    (((int) (uniqueEmptyRows.get(2) - uniqueEmptyRows.get(1))))));
            characterWidth = (uniqueEmptyRows.get(2) - uniqueEmptyRows.get(1)) * 0.92;
            singleLines.add(cutSingleCha(cutMat, true));
            Imgcodecs.imwrite("C:/Users/visea/Desktop/test/new/temp/Index" +
                            String.valueOf(picId) + String.valueOf(++colNum) + ".jpg"
                    , cutMat);
            System.out.print("Character Width:");
            System.out.println(characterWidth);
        } else {

            getUniqueLines(emptyRows, uniqueEmptyRows, 10);

            if (emptyRows.get(0) > mat.height() * 0.3) uniqueEmptyRows.add(0, 0.0);
            if (emptyRows.get(emptyRows.size() - 1) < mat.height() * 0.75) {
                uniqueEmptyRows.add((double) mat.height());
            }

            showLines(mat, uniqueEmptyRows, false);
            for (int i = 0; i < uniqueEmptyRows.size(); i++) {
                if (i != uniqueEmptyRows.size() - 1) {
                    Mat cutMat = new Mat(mat, new Rect(0,
                            (uniqueEmptyRows.get(i).intValue()),
                            mat.width(),
                            (((int) (uniqueEmptyRows.get(i + 1) - uniqueEmptyRows.get(i))))));
                    if (dataType == RowInfo.IS_STRING)
                        singleLines.add(cutSingleCha(cutMat, false));
                    else {
                        ArrayList<Mat> line = new ArrayList<>();
                        line.add(cutMat);
                        singleLines.add(line);
                    }
                }
            }
        }

        ArrayList<Mat> allCharacters = new ArrayList<>();

        for (ArrayList<Mat> line : singleLines) {
            for (Mat character : line) {
                allCharacters.add(character);
            }
        }

        return allCharacters;
    }

    /**
     * cut the Chinese lines to single characters
     * similar with cutSingleLines above
     *
     * @param srcMat source image
     * @return list of characters
     */
    private ArrayList<Mat> cutSingleCha(Mat srcMat, boolean isIndex) {
        ArrayList<Mat> characters = new ArrayList<>();
        ArrayList<Double> emptyCols = new ArrayList<>();
        ArrayList<Double> uniqueEmptyCols = new ArrayList<>();
        double[] points;
        for (int col = 0; col < srcMat.cols(); col++) {
            int count = 0;
            for (int row = 5; row < srcMat.rows() - 5; row++) {
                points = srcMat.get(row, col);
                if (points[0] == 255) {
                    count++;
                }
            }
            if (count < 1) emptyCols.add((double) col);
        }

        getBorders(emptyCols, uniqueEmptyCols, 5, 0);
        showLines(srcMat, uniqueEmptyCols, true);
        //cut the image according to the left and right borders
        for (int i = 1; i < uniqueEmptyCols.size() - 1; i += 2) {
            Mat cutMat;

            double chaWidth = 0;
            int iStart = i;
            int iEnd = i + 1;
            while (i < uniqueEmptyCols.size() - 3 &&
                    chaWidth + uniqueEmptyCols.get(i + 1) - uniqueEmptyCols.get(i) < characterWidth * 0.8 &&
                    chaWidth + uniqueEmptyCols.get(i + 3) - uniqueEmptyCols.get(i) < characterWidth * 1.2
                    ) {
                chaWidth += uniqueEmptyCols.get(i + 1) - uniqueEmptyCols.get(i);
                i += 2;
                iEnd = i + 1;
            }


            cutMat = new Mat(srcMat, new Rect(uniqueEmptyCols.get(iStart).intValue(),
                    0,
                    (int) (uniqueEmptyCols.get(iEnd) - uniqueEmptyCols.get(iStart)),
                    srcMat.height()));

            if (isIndex) {
                Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(2, 2));
                Imgproc.erode(cutMat, cutMat, kernelErode);
            }

            Imgcodecs.imwrite("C:/Users/visea/Desktop/test/new/cutSingleCha/" +
                            String.valueOf(picId) + String.valueOf(++colNum) + ".jpg"
                    , cutMat);
            characters.add(cutMat);
        }
        return characters;
    }

    /**
     * get the left and right borders of some continuous lines
     *
     * @param src         source of coordinates list
     * @param dst         destination of coordinates list, the border will be stored like (start - end - start - end ...)
     * @param maxGap      the maximum gap between adjacent lines that can be considered as continuous lines
     * @param minDistance the minimum distance between start border and end border
     */
    private void getBorders(ArrayList<Double> src, ArrayList<Double> dst, int maxGap, int minDistance) {
        Collections.sort(src);
        for (int i = 0; i < src.size(); i++) {
            double start = src.get(i);
            double end = src.get(i);
            //when the distance between two lines less than 10,get the average of them
            while (i != src.size() - 1 && src.get(i + 1) - src.get(i) < maxGap) {
                end = src.get(i + 1);
                i++;
            }
            if (end - start >= minDistance) {
                dst.add(start);
                dst.add(end);
            }
        }
    }

    /**
     * if the gap between lines less than filterGap*width, reserve the first of them and filter out others
     *
     * @param src source coordinates list
     * @param dst destination coordinates list
     */
    private void filterLines(ArrayList<Double> src, ArrayList<Double> dst) {
        for (int i = 0; i < src.size(); i++) {
            int recode = i;
            while (i != src.size() - 1 && src.get(i + 1) - src.get(i) <
                    5 * characterWidth) {
                i++;
            }
            dst.add(src.get(recode));
        }
    }


    private void showLines(Mat mat, ArrayList<Double> lineCoordinates, boolean isX) {
        Mat rgbMat = new Mat();
        Imgproc.cvtColor(mat, rgbMat, COLOR_GRAY2RGB);
        for (double coordinate : lineCoordinates) {
            Point pt1, pt2;
            if (isX) {
                pt1 = new Point(coordinate, 0);
                pt2 = new Point(coordinate, srcPic.height());
            } else {
                pt1 = new Point(0, coordinate);
                pt2 = new Point(srcPic.width(), coordinate);
            }
            Imgproc.line(rgbMat, pt1, pt2, new Scalar(0, 0, 255), 2);
        }
        Imgcodecs.imwrite("C:/Users/visea/Desktop/test/new/showLines/" +
                        String.valueOf(picId) + String.valueOf(++colNum) + ".jpg"
                , rgbMat);

    }

    /**
     * filter the source coordinates, if some values are too close ,get the average of them
     *
     * @param src    source coordinates list
     * @param dst    destination coordinate list
     * @param minGap the minimum gap between coordinates
     */
    private void getUniqueLines(ArrayList<Double> src, ArrayList<Double> dst, int minGap) {
        Collections.sort(src); //sort the sourc `e coordinates list
        for (int i = 0; i < src.size(); i++) {
            double sum = src.get(i);
            double num = 1;
            //when the distance between lines less than minGap, get the average of them
            while (i != src.size() - 1 && src.get(i + 1) - src.get(i) < minGap) {
                num++;
                sum = sum + src.get(i + 1);
                i++;
            }
            if (num == 1) {
                dst.add(src.get(i));
            } else {
                dst.add(((sum / num)));
            }
        }
    }

    /**
     * convert Mat format to bufferedImage format for OCR
     *
     * @param mat
     * @return
     */
    private BufferedImage convertMatToBufferedImage(Mat mat) {
        BufferedImage bufferedImage = new BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_BYTE_GRAY);
        byte[] data = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        mat.get(0, 0, data);
        return bufferedImage;
    }

    private ArrayList<BufferedImage> convertMatsToBufferedImages(ArrayList<Mat> mats) {
        ArrayList<BufferedImage> bufferedImages = new ArrayList<>();
        for (Mat mat : mats) {
            bufferedImages.add(convertMatToBufferedImage(mat));
        }
        return bufferedImages;
    }

    /*private void testParams() {
        for (int param1 = 17; param1 < 40; param1 += 2) {
            for (int param2 = 2; param2 < 20; param2 += 1) {
                BLOCK_SIZE = param1;
                C_THRESHOLD = param2;
                Mat newPic = new Mat();
                Imgproc.adaptiveThreshold(srcPic, newPic, 255, ADAPTIVE_THRESH_MEAN_C,
                        THRESH_BINARY_INV, BLOCK_SIZE, C_THRESHOLD);
                Imgcodecs.imwrite("C:/Users/visea/Desktop/test/new/binarization/" + String.valueOf(param1) + "_" +
                        String.valueOf(param2) + ".jpg",newPic);
            }
        }
    }*/
    interface RecognizeCharacters {
        String recognize(ArrayList<BufferedImage> bufferedImages, int dataType, boolean isIndex);
    }
}


