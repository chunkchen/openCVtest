package xyz.viseator;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Created by viseator on 2016/12/5.
 */
public class TableInfo {
    private int colsSize;
    private static final double DEFAULT_FILTERGAP = 0;
    public static final int DATA_TYPE_NUMBER_0_1 = 0;
    public static final int DATA_TYPE_NUMBER_1_2 = 1;
    public static final int DATA_TYPE_STRING_0_1 = 2;
    public static final int DATA_TYPE_STRING_1_2 = 3;
    private ArrayList<ColumnInfo> cols;

    public TableInfo(int colsSize) {
        this.colsSize = colsSize;
        cols = new ArrayList<>();
        for (int i = 0; i < colsSize; i++) {
            cols.add(new ColumnInfo());
        }
    }

    public void initColumn(int position, int dataType) {
        ColumnInfo columnInfo = cols.get(position);
        columnInfo.setFilterGap(DEFAULT_FILTERGAP);
        switch (dataType) {
            case DATA_TYPE_NUMBER_0_1:
                columnInfo.setDataType(dataType);
                columnInfo.setBoundLeft(0);
                columnInfo.setBoundRight(1);
                break;
            case DATA_TYPE_NUMBER_1_2:
                columnInfo.setDataType(dataType);
                columnInfo.setBoundLeft(1);
                columnInfo.setBoundRight(2);
                break;

            case DATA_TYPE_STRING_0_1:
                columnInfo.setDataType(dataType);
                columnInfo.setBoundLeft(0);
                columnInfo.setBoundRight(1);
                columnInfo.setFilterGap(0.3);
                break;

            case DATA_TYPE_STRING_1_2:
                columnInfo.setDataType(dataType);
                columnInfo.setBoundLeft(1);
                columnInfo.setBoundRight(2);
                break;
        }
    }

    public int getColsSize() {
        return colsSize;
    }


    public ColumnInfo getCols(int position) {
        return cols.get(position);
    }

    public void setCols(ArrayList<ColumnInfo> cols) {
        this.cols = cols;
    }

    public class ColumnInfo {

        private double filterGap;
        private int dataType;
        private int boundLeft;
        private int boundRight;
        private ArrayList<BufferedImage> bufferedImages;
        private String result;

        public double getFilterGap() {
            return filterGap;
        }

        public void setFilterGap(double filterGap) {
            this.filterGap = filterGap;
        }

        public BufferedImage getBufferedImage(int position) {
            return bufferedImages.get(position);
        }

        public int getChaSize() {
            return bufferedImages.size();
        }

        public void setBufferedImages(ArrayList<BufferedImage> bufferedImages) {
            this.bufferedImages = bufferedImages;
        }

        public int getDataType() {
            return dataType;
        }

        public void setDataType(int dataType) {
            this.dataType = dataType;
        }

        public int getBoundLeft() {
            return boundLeft;
        }

        public void setBoundLeft(int boundLeft) {
            this.boundLeft = boundLeft;
        }

        public int getBoundRight() {
            return boundRight;
        }

        public void setBoundRight(int boundRight) {
            this.boundRight = boundRight;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

    }
}
