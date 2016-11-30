package xyz.viseator;import org.opencv.core.*;import org.opencv.imgcodecs.Imgcodecs;import org.opencv.imgproc.Imgproc;import java.util.ArrayList;import java.util.Collections;import static org.opencv.imgproc.Imgproc.*;/** * Created by viseator on 2016/11/13. */public class ProgressPic {    private Mat srcPic;//Source Picture    private Mat rawSrcPic;//Raw Source Picture    private Mat rgbSrcPic;//Rgb Source Picture    private Mat dilateMuchPic;//Dilate Much for finding lines    private String path;    private double scaleSize = 0.5;    private ArrayList<Double> uniqueLineYs;    private ArrayList<Mat> blockImages;    private int mark;    public void progress(String path, int mark) {        this.mark = mark;        this.path = path;        rawSrcPic = Imgcodecs.imread(path, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);        rgbSrcPic = Imgcodecs.imread(path, Imgcodecs.CV_LOAD_IMAGE_COLOR);     /*   srcPic = new Mat();        srcPic.create((int) (rawSrcPic.rows() * scaleSize), (int) (rawSrcPic.cols() * scaleSize), CvType.CV_8UC1);        Imgproc.resize(rawSrcPic,srcPic,srcPic.size());*/        srcPic = rawSrcPic;        toGrayAndBinarization();        deNoise();        findLines();        cutImages();    }    private void toGrayAndBinarization() {//        Test for finding the best param1 and param2/*        for (int param1 = 5; param1 < 40; param1 += 6) {            for (int param2 = 0; param2 <= 40; param2 += 5) {                Imgproc.adaptiveThreshold(srcPic, newPic, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, param1, param2);                Imgcodecs.imwrite("/storage/sdcard/pic/test/2_" + String.valueOf(param1) +                                "_" + String.valueOf(param2)+                        ".jpg", newPic);            }        }*/        Imgproc.adaptiveThreshold(srcPic, srcPic, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY_INV, 27, 10);/*        for (testId = 0; testId < 9; testId++) {            Imgcodecs.imwrite("/storage/sdcard/pic/test1/" + String.valueOf(testId) +                    ".jpg", newPic);        }*///        Imgcodecs.imwrite("C:/Users/visea/Desktop/test/java/afterBinarization.jpg", srcPic);    }    private void deNoise() {        Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(2, 2));        Imgproc.erode(srcPic, srcPic, kernelErode);//        Imgcodecs.imwrite("C:/Users/visea/Desktop/test/java/afterErode.jpg", srcPic);        Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(2, 2));        Imgproc.dilate(srcPic, srcPic, kernelDilate);        dilateMuchPic = new Mat();        Mat kernelDilateMuch = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(12, 12));        Imgproc.dilate(srcPic, dilateMuchPic, kernelDilateMuch);        Imgcodecs.imwrite("C:/Users/visea/Desktop/test/java/afterDilate.jpg", srcPic);    }    private void findLines() {        ArrayList<Double> lineYs = new ArrayList<>();        uniqueLineYs = new ArrayList<>();        Mat lines = new Mat();        Mat showLines = new Mat();        showLines.create(srcPic.rows(), srcPic.cols(), CvType.CV_32SC3);        Imgproc.HoughLinesP(dilateMuchPic, lines, 1, Math.PI / 180, 150, 1200, 20);        System.out.println(lines.rows());        for (int i = 0; i < lines.rows(); i++) {            double[] points = lines.get(i, 0);            double y1, y2;            y1 = points[1];            y2 = points[3];            if (Math.abs(y1 - y2) < 30) {                lineYs.add((y1 + y2) / 2);            }        }        System.out.println(lineYs.size());        Collections.sort(lineYs);        //Solve Lines own same liens        for (int i = 0; i < lineYs.size(); i++) {            double sum = lineYs.get(i);            double num = 1;            //When the distance between two lines less than 10,get the average of them            while (i != lineYs.size() - 1 && lineYs.get(i + 1) - lineYs.get(i) < 10) {                num++;                sum = sum + lineYs.get(i + 1);                i++;            }            if (num == 1) {                uniqueLineYs.add(lineYs.get(i));            } else {                uniqueLineYs.add(sum / num);            }        }        for (double y : uniqueLineYs) {            Point pt1 = new Point(0, y);            Point pt2 = new Point(srcPic.width(), y);            System.out.println(y);            Imgproc.line(rgbSrcPic, pt1, pt2, new Scalar(0, 0, 255), 3);        }        Imgcodecs.imwrite("C:/Users/visea/Desktop/test/java/findLines.jpg", rgbSrcPic);    }    private double padding = 0;    private double paddingLR = 0.07;    private void cutImages() {        blockImages = new ArrayList<>();        for (int i = 0; i < uniqueLineYs.size(); i++) {            Rect rect;            double y = uniqueLineYs.get(i);            if (i != uniqueLineYs.size() - 1) {                rect = new Rect((int) (srcPic.width() * paddingLR),                        (int) (y + (uniqueLineYs.get(i + 1) - y) * padding),                        (int) (srcPic.width() * (1 - paddingLR * 2)),                        (int) ((uniqueLineYs.get(i + 1) - y) * (1 - padding * 2)));            } else {                rect = new Rect((int) (srcPic.width() * paddingLR),                        (int) (y + (srcPic.height() - y) * padding),                        (int) (srcPic.width() * (1 - paddingLR * 2)),                        (int) ((srcPic.height() - y) * (1 - padding * 2)));            }            blockImages.add(new Mat(srcPic, rect));        }        int outNum = 0;        System.out.println(blockImages.size());        for (Mat image : blockImages) {            Mat lines = new Mat();            Mat showLines = new Mat();            ArrayList<Double> lineXs = new ArrayList<>();            Imgproc.cvtColor(image, showLines, COLOR_GRAY2BGR);            dilateMuchPic = new Mat();            Mat kernelDilateMuch = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(6, 6));            Imgproc.dilate(image, dilateMuchPic, kernelDilateMuch);            Imgproc.HoughLinesP(dilateMuchPic, lines, 0.1, Math.PI / 360, 0,                    image.height() * 0.75, 15);            System.out.print("Lines rows:");            System.out.println(lines.rows());            for (int i = 0; i < lines.rows(); i++) {                double[] points = lines.get(i, 0);                double x1, x2;                x1 = points[0];                x2 = points[2];                if (Math.abs(x1 - x2) < 50) {                    System.out.println(String.valueOf(x1) + "," + String.valueOf(x2));                    lineXs.add((x1 + x2) / 2);                }            }            Collections.sort(lineXs);            ArrayList<Double> uniqueLineXs = new ArrayList<>();            for (int i = 0; i < lineXs.size(); i++) {                double sum = lineXs.get(i);                double num = 1;                //When the distance between two lines less than 10,get the average of them                while (i != lineXs.size() - 1 && lineXs.get(i + 1) - lineXs.get(i) < 10) {                    num++;                    sum = sum + lineXs.get(i + 1);                    i++;                }                if (num == 1) {                    uniqueLineXs.add(lineXs.get(i));                } else {                    uniqueLineXs.add(sum / num);                }            }            ArrayList<Double> betterLineXs = new ArrayList<>();            for (int i = 0; i < uniqueLineXs.size(); i++) {                int recode = i;                while (i != uniqueLineXs.size() - 1 && uniqueLineXs.get(i + 1) - uniqueLineXs.get(i) < 100) {                    i++;                }                betterLineXs.add(uniqueLineXs.get(recode));            }            if (betterLineXs.size() < 2 | betterLineXs.size() > 5) {                continue;            }            for (double x : betterLineXs) {                Point pt1 = new Point(x, 0);                Point pt2 = new Point(x, image.height());                System.out.println(x);                Imgproc.line(showLines, pt1, pt2, new Scalar(0, 0, 255), 3);            }            Imgcodecs.imwrite("C:/Users/visea/Desktop/test/java/cut2/" +                    String.valueOf(++outNum) +                    ".jpg", showLines);/*            Mat cuttedMat;            if (betterLineXs.size() == 2 | betterLineXs.size() == 4) {                cuttedMat = new Mat(image, new Rect((int) (betterLineXs.get(0) + 5),                        0, (int) (betterLineXs.get(1) - betterLineXs.get(0) - 15), image.height()));            } else if (betterLineXs.size() == 5) {                cuttedMat = new Mat(image, new Rect((int) (betterLineXs.get(1) + 5),                        0, (int) (betterLineXs.get(2) - betterLineXs.get(1) - 15), image.height()));            } else {                cuttedMat = new Mat();            }            Imgcodecs.imwrite("C:/Users/visea/Desktop/test/java/cut2/" +                    String.valueOf(++outNum) +                    ".jpg", cuttedMat);*/        }    }}