package xyz.viseator;

import org.opencv.core.Core;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Wudi
 * viseator@gmail.com
 * Created by viseator on 2016/12/12.
 */
public class WudiTest {
    private static String EXCEL_PATH = "./source/交付表格.xlsx";
    private static String OUTPUT_PATH = "./交付表格.xlsx";

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String args[]) {
        CutPic cutPic = new CutPic();
        OCR ocr = new OCR("C:\\Program Files (x86)\\Tesseract-OCR", "./dic.txt", cutPic.getIndexes());
        cutPic.setOcr(new CutPic.RecognizeCharacters() {
            @Override
            public String recognize(ArrayList<BufferedImage> bufferedImages, int dataType, boolean isIndex) {
                return ocr.execute(bufferedImages, dataType, isIndex);
            }
        });
//        for (int picId = 1; picId <= 49; picId++) {
//        int picId = 31;
//            cutPic.progress("./image/1 (" + String.valueOf(picId) + ").jpg", picId);
//        }
        ArrayList<RowInfo> rowInfos;
        rowInfos = cutPic.progress("./image/test.jpg", 1);
        OutputExcel outputExcel = new OutputExcel(EXCEL_PATH, OUTPUT_PATH);
        outputExcel.storeResultToExcel(rowInfos);
//        IndexReader indexReader = new IndexReader("./index.txt");
//        System.out.println(indexReader.getRowInfo("舒张压").getLeftBorder() + " " + indexReader.getRowInfo("舒张压").getRightBorder());
    }
}
