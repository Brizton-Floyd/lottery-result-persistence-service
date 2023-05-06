package com.floyd.lottoptions.agr.documentreaders;

import com.floyd.lottoptions.agr.config.LotteryUrlConfig;

import com.floyd.lottoptions.agr.documentreaders.aggregators.DataAggregator;
import com.floyd.lottoptions.agr.documentreaders.aggregators.DataAggregatorFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class PdfFileReader implements FileReader {
    private static List<String> lines = new ArrayList<>();
    private String lottoStateName;

    @Override
    public List<String[]> getFileContents(LotteryUrlConfig.GameInfo gameInfo) throws IOException{
        List<String[]> data = new ArrayList<>();
        PDDocument document = null;


        // clear the lines array since its static
        lines.clear();

        try {
            byte[] pdfByteContent = getPdfByteContent(gameInfo.getUrl());
            document = Loader.loadPDF(pdfByteContent);
            PDFTextStripper stripper = new GetCharLocationAndSize();
            stripper.setSortByPosition( true );
            stripper.setStartPage( 0 );
            stripper.setEndPage( document.getNumberOfPages() );

            Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
            stripper.writeText(document, dummy);
        }
        finally {
            if( document != null ) {
                document.close();
            }
        }

        DataAggregator aggregator = DataAggregatorFactory.getDataAggregator(lottoStateName,
            gameInfo.getName(), lines);
        if (aggregator != null)
            aggregator.populateData(data);

        return data;
    }

    @Override
    public void setLottoStateName(String lottoStateName) {
        this.lottoStateName = lottoStateName;
    }

    private byte[] getPdfByteContent(String url) throws IOException{
        InputStream input = new URL(url).openStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[131072];
        int n = 0;
        while (-1!=(n=input.read(buf)))
        {
            out.write(buf, 0, n);
        }
        out.close();
        input.close();

        return out.toByteArray();
    }

    private static class GetCharLocationAndSize extends PDFTextStripper {

        public GetCharLocationAndSize() throws IOException {
        }

        /**
         * Override the default functionality of PDFTextStripper.writeString()
         */
        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            lines.add(string);
        }
    }
}
