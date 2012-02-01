/*
 * Created on 05.11.2004
 * Color Structure Descriptor for MPEG7 VizIR
 *
 * Copyright (C) 2004 Adis Buturovic, Vienna University of Technology
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Contact address: adis@ims.tuwien.ac.at
 * For full description see MPEG7-CSD.pdf
 */

package org.vizir.descriptor.csd;

import org.jdom.*;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.input.SAXBuilder;
import java.awt.image.*;
import java.io.FileOutputStream;

import org.vizir.media.*;
import org.vizir.*;

/**
 * Contains all necessery methods for feature extraction from media content using Color structure descriptor
 * @author adis@ims.tuwien.ac.at
 * @see MPEG7-CSD.pdf
 */

public class ColorStructureDescriptor implements Descriptor {

    /** Should the result be written to a XML file on the disk? */

    private static boolean FILEOUTPUT = true;

    /** Filename of the output XML */

    private static String XMLfileName = "CSD.xml";

    /** Quantity of color quantization bins */

    private static int M = 0;

    /** Subspaces needed for the quantization */

    private static int subspace = 0;

    /** Color Structure Histogram */

    private float[] ColorHistogram = null;

    /**
    * Quantization table for 256, 128, 64 and 32 quantisation bins.
    *
    * <br>form:
    * <code><br>
    * subspace0 , subspace1 , subspace2 , subspace3 , subspace4 <br>
    *  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum   - 256 Levels [offset pos  0 - pos  9]<br>
    *  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum   - 128 Levels [offset pos 10 - pos 19]<br>
    *  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum   -  64 Levels [offset pos 20 - pos 29]<br>
    *  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum  ,  Hue,Sum   -  32 Levels [offset pos 30 - pos 39]<br>
    * </code>
    * @see HMMDColorStuctureExtraction
    */

    private static final int[] quantTable = {   1,32, 4,8, 16,4, 16,4, 16,4,            // Hue, Sum - subspace 0,1,2,3,4 for 256 levels
                                            1,16, 4,4,  8,4,  8,4,  8,4,            // Hue, Sum - subspace 0,1,2,3,4 for 128 levels
                                            1,8,  4,4,  4,4,  8,2,  8,1,            // Hue, Sum - subspace 0,1,2,3,4 for  64 levels
                                            1,8,  4,4,  4,4,  4,1,  4,1     };      // Hue, Sum - subspace 0,1,2,3,4 for  32 levels

    /**
    * xsiXMLns is constant for XML Namespace xmlns:xsi = "urn:mpeg:mpeg7:schema:2001"
    */

    private static final Namespace xsiXMLns = Namespace.getNamespace("xsi","http://www.w3.org/2001/XMLSchema-instance");

    /**
    * xsiXMLns is constant for XML Namespace xmlns:xsi = "urn:mpeg:mpeg7:schema:2001"
    */

    private static final Namespace xsiSLXMLns = Namespace.getNamespace("schemaLocation","urn:mpeg:mpeg7:schema:2001 .\\Mpeg7-2001.xsd");

    /**
    * XMLns is constant for XML Namespace xmlns = "http://www.mpeg7.org/2001/MPEG-7_Schema"
    */

    private static final Namespace XMLns = Namespace.getNamespace("urn:mpeg:mpeg7:schema:2001");

    /**
     * The <code>extractFeature(MediaContent)</code> class loads the media content, convert it to HMMD color space and executes
     * the CSD extraction and quantization.
     *
     * @param MediaContent
     * @throws Exception
     */

    public void extractFeature(MediaContent mc) throws Exception
    {

        // load image to BufferedImage

        BufferedImage targetImage = mc.getImage();
        double height = mc.getSize().getHeight();
        double width = mc.getSize().getWidth();

        int temp[][] =  new int[(int)height - 1][(int)width - 1];

        if (width > height) {System.out.println("\nExit - vizir bug: file unsupported -> MediaFrame.getPixelAt"); System.exit(0);  }

        MediaFrame mf = new MediaFrame(targetImage);

        int ir[][] = temp;
        int ig[][] = temp;
        int ib[][] = temp;

        int iH[][]    = temp;
        int iMax[][]  = temp;
        int iMin[][]  = temp;
        int iDiff[][] = temp;
        int iSum[][]  = temp;

        // convert BufferedImage to double int array for every RGB color
        // ant then covert RGB values into HMMD

        for (int ch = 0; ch < (int)height - 1; ch++) {
            for (int cw = 0; cw < (int)width - 1; cw++) {
                ir[ch][cw] = mf.getPixelAt(ch,cw).getComponent(0); // RED
                ig[ch][cw] = mf.getPixelAt(ch,cw).getComponent(1); // GREEN
                ib[ch][cw] = mf.getPixelAt(ch,cw).getComponent(2); // BLUE

                int[] tempHMMD = RGB2HMMD(ir[ch][cw],ig[ch][cw],ib[ch][cw]);
                iH[ch][cw]   = tempHMMD[0];                     // H
                iMax[ch][cw] = tempHMMD[1];                     // Max
                iMin[ch][cw] = tempHMMD[2];                         // Min
                iDiff[ch][cw]= tempHMMD[3];                         // Diff
                iSum[ch][cw] = tempHMMD[4];                         // Sum
                }
            }

        ColorHistogram = HMMDColorStuctureExtraction(iH, iMax, iMin, iDiff, iSum, (int)height, (int)width); // extract HMMD colors and make histogram

        // if ( M != 256 ) ColorHistogram = reQuantization(ColorHistogram); // requantize and normalize histogram to 0-255 range
        ColorHistogram = reQuantization(ColorHistogram);
    }

    /**
     * The <code>HMMDColorStuctureExtraction(int[][], int[][], int[][], int[][], int[][], int, int)</code> class builds the Color Structure Histogram.
     *
     * @param iH - Hue values of the frame
     * @param iMax - Max values of the frame
     * @param iMin - Min values of the frame
     * @param iDiff - Diff values of the frame
     * @param iSum - values of the frame
     * @param height - height of the frame
     * @param width - width of the frame
     * @return float[] - Color Structure Histogram
     * @throws Exception
     */

    private static float[] HMMDColorStuctureExtraction(int iH[][], int iMax[][], int iMin[][], int iDiff[][], int iSum[][], int height, int width) throws Exception
        {
            long hw = height * width;
            long p = Math.round(0.5 * Math.log(hw)) - 8;

            if (p < 0) p = 0;
            double K = Math.pow(2,p);

            double E = 8 * K;
            int m = 0;

            width--; height--; // um ueberlauf in der schleife zu vermeiden - 24.07.2005

            if (M == 0) { setQuantizationLevels(64); System.out.println("WARNING: quantization size will be set to default: 64");} // default value is 64

            float h[] = new float[M];       // CSD temp
            int t[] = new int[M];       // CSD temp - int sollte stimmen

            for (int i = 0; i < M; i++) { /* t[i] = 0; */ h[i] = 0.0f; }

            for(int y = 0; y < ((height-E)+1); y+=K) {
                 for(int x = 0; x < ((width-E)+1); x+=K) {
                     // re initialize the local histogram t[m]
                     for(m = 0; m < M; m++) t[m] = 0;

                     //collect local histogram over pixels in structuring element
                     for(int yy = y; yy < y+E; yy+=K)
                         for(int xx = x; xx < x+E; xx += K)
                            {
                                // get quantized pixel color and update local histogramm

                                // The 256-cell color space is quantized non-uniformly as follows.

                                // First, the HMMD Color space is divided into 5 subspaces:
                                // subspaces 0,1,2,3, and 4.
                                // This subspace division is defined along the Diff(colorfulness) axis of the HMMD Color space.
                                // The subspaces are defined by cut-points which determine the following diff axis intervals:
                                // [(0,6),(6,20),(20,60),(60,110),(110,255)].

                                // Second, each color subspace is uniformly quantized along Hue and Sum axes, where the
                                // number of Quantization levels along each axis is defined in the Table for each operating point.
                                //
                                // Example:
                                //              64 levels                   32 levels
                                //
                                //  Subspace    Hue     Sum             Hue         Sum
                                //      0       1       8               1           8
                                //      1       4       4               4           4
                                //      2       4       4               4           4
                                //      3       8       2               4           1
                                //      4       8       1               4           1

                                int offset = 0; // offset position in the quantization table

                                int q = 0;
                                try {

                                // define the subspace along the Diff axis

                                if (iDiff[yy][xx] < 7)                                      subspace = 0;
                                    else if ((iDiff[yy][xx] > 6) && (iDiff[yy][xx]  < 21))      subspace = 1;
                                    else if ((iDiff[yy][xx] > 19) && (iDiff[yy][xx] < 61))      subspace = 2;
                                    else if ((iDiff[yy][xx] > 59) && (iDiff[yy][xx] < 111))     subspace = 3;
                                    else if ((iDiff[yy][xx] > 109) && (iDiff[yy][xx] < 256))    subspace = 4;

                                // HMMD Color Space quantization
                                // see MPEG7-CSD.pdf

                                if (M == 256)
                                    {
                                        offset = 0;
                                        //m = (int)(((float)iH[yy][xx] / M) * quantTable[offset+subspace] + ((float)iSum[yy][xx] / M) * quantTable[offset+subspace+1]);
                                        m = (int)((iH[yy][xx] / M) * quantTable[offset+subspace] + (iSum[yy][xx] / M) * quantTable[offset+subspace+1]);
                                    }
                                else if (M == 128)
                                    {
                                        offset = 10;
                                        //m = (int)(((float)iH[yy][xx] / M) * quantTable[offset+subspace] + ((float)iSum[yy][xx] / M) * quantTable[offset+subspace+1]);
                                        m = (int)((iH[yy][xx] / M) * quantTable[offset+subspace] + (iSum[yy][xx] / M) * quantTable[offset+subspace+1]);
                                    }
                                else if (M == 64)
                                    {
                                        offset = 20;
                                        //m = (int)(((float)iH[yy][xx] / M) * quantTable[offset+subspace] + ((float)iSum[yy][xx] / M) * quantTable[offset+subspace+1]);
                                        m = (int)((iH[yy][xx] / M) * quantTable[offset+subspace] + (iSum[yy][xx] / M) * quantTable[offset+subspace+1]);

                                    }
                                else if (M == 32)
                                    {
                                        offset = 30;
                                        //m = (int)(((float)iH[yy][xx] / M) * quantTable[offset+subspace] + ((float)iSum[yy][xx] / M) * quantTable[offset+subspace+1]);
                                        m = (int)((iH[yy][xx] / M) * quantTable[offset+subspace] + (iSum[yy][xx] / M) * quantTable[offset+subspace+1]);

                                        // System.out.println("m: " + m);

                                    }


                                t[m]++;

                                }
                                catch(Exception e) { System.out.println("PROB? - quant. schleife: x = " + xx + " y = " + yy); }
                            }

                         // increment the color structure histogramm for each color present in the structuring element
                         for(m = 0; m < M; m++)
                            {
                                if(t[m] > 0) h[m]++;
                            }
                         }
                     }

                int S = (width-(int)E+(int)K)/(int)K*((height-(int)E+(int)K)/(int)K);
                for(m = 0; m < M; m++)
                    {
                        h[m] = h[m] / S;
                    }
                return h;
        }

    /**
     * The <code>reQuantization(float[] colorHistogramTemp)</code> class is responsible for re-quantization of
     * the CSD Histogram and normalizing to 8-bit code values
     * @param float[] - Color Structure Histogram in the range [0, 1]
     * @return float[] - normalized Color Structure Histogram
     */

    private float[] reQuantization(float[] colorHistogramTemp) {

        float[] uniformCSD = new float[colorHistogramTemp.length];

        for (int i=0; i < colorHistogramTemp.length; i++)
        {
            // System.out.print(colorHistogramTemp[i] + " ");
            // System.out.println(" --- ");

            if (colorHistogramTemp[i] == 0) uniformCSD[i] = 0; //
            else if (colorHistogramTemp[i] < 0.000000001f) uniformCSD[i] = (int)Math.round( ( ((float)colorHistogramTemp[i] - 0.32f ) / ( 1f - 0.32f ) ) * 140 + (115 - 35 - 35 - 20 - 25 - 1) );    // (int)Math.round((1f / 0.000000001f) * (float)colorHistogramTemp[i]);
            else if (colorHistogramTemp[i] < 0.037f) uniformCSD[i] = (int)Math.round( ( ((float)colorHistogramTemp[i] - 0.32f ) / ( 1f - 0.32f ) ) * 140 + (115 - 35 - 35 - 20 - 25));
            else if (colorHistogramTemp[i] < 0.08f) uniformCSD[i] = (int)Math.round( ( ((float)colorHistogramTemp[i] - 0.32f ) / ( 1f - 0.32f ) ) * 140 + (115 - 35 - 35 - 20));
            else if (colorHistogramTemp[i] < 0.195f) uniformCSD[i] = (int)Math.round( ( ((float)colorHistogramTemp[i] - 0.32f ) / ( 1f - 0.32f ) ) * 140 + (115 - 35 - 35));
            else if (colorHistogramTemp[i] < 0.32f) uniformCSD[i] = (int)Math.round( ( ((float)colorHistogramTemp[i] - 0.32f ) / ( 1f - 0.32f ) ) * 140 + (115 - 35));
            else if (colorHistogramTemp[i] > 0.32f) uniformCSD[i] = (int)Math.round( ( ((float)colorHistogramTemp[i] - 0.32f ) / ( 1f - 0.32f ) ) * 140 + 115);
            else uniformCSD[i] = (int)Math.round((255f / 1f) * (float)colorHistogramTemp[i]);

        }

        return uniformCSD;
    }

    /**
     * The <code>RGB2HMMD (int ir, int ig, int ib)</code> class is responsible for converting RGB values to HMMD values
     * @param ir - RED component
     * @param ig - GREEN component
     * @param ib - BLUE component
     * @return int[] - HMMD value of the pixle
     * @author adis@ims.tuwien.ac.at
     * @throws Exception
     */

    private static int[] RGB2HMMD (int ir, int ig, int ib) throws Exception
    {
        int HMMD[] = new int[5];

        float max = (float)Math.max(Math.max(ir, ig), Math.max(ig, ib));
        float min = (float)Math.min(Math.min(ir, ig), Math.min(ig, ib));

        float diff = (max - min);
        float sum = (float) ((max + min)/2.);

        float hue = 0;
        if (diff == 0) hue = 0;
          else if (ir == max && (ig - ib) > 0)  hue = 60*(ig-ib) / (max-min);
          else if (ir == max && (ig - ib) <= 0) hue = 60*(ig-ib) / (max-min) + 360;
          else if (ig == max) hue = (float) (60*(2.+(ib-ir) / (max-min)));
          else if (ib == max) hue = (float) (60*(4.+(ir-ig) / (max-min)));

          diff /= 2;

          HMMD[0] = (int)(hue);
          HMMD[1] = (int)(max);
          HMMD[2] = (int)(min);
          HMMD[3] = (int)(diff);
          HMMD[4] = (int)(sum);

          return (HMMD);
    }

    /**
     * The <code>setDescriptionFromString(String xmlString)</code> class is responsible for reading
     * XML Documents
     * @param xmlString
     * @throws Exception
     */

    public void setDescriptionFromString(String xmlString) throws Exception{
         SAXBuilder builder = new SAXBuilder();
         java.io.StringReader XMLsr = new java.io.StringReader(xmlString);
         Document doc = builder.build(XMLsr);
         Element root = doc.getRootElement();

         if (root.getName().equals("Mpeg7")){
             Element child = root.getChild("Description",XMLns);
         }
         else throw new Exception("XML format error: not Mpeg7 Descriptor");
    }

    /**
   * The <code>getDescriptionAsString()</code> class is responsible for writing XML Documents.
   * XMLOutputter method helps to serialize the document and stores it into a String. Encoding is set to iso-8859-1
   * @throws Exception
   */

   public String getDescriptionAsString() throws Exception {

        String Histogram = new String("");                              // String containing ColorStructureDescriptor histogramm for XML Output

        Element root = new Element("Mpeg7", XMLns);
        Element description = new Element("Description");
        Element mmediaContent = new Element("MultimediaContent");
        Element image = new Element("Image");
        Element visualDescriptor = new Element("VisualDescriptor");
        Element histogramm = new Element("Values");

        root.addNamespaceDeclaration(xsiXMLns);
        root.addNamespaceDeclaration(xsiSLXMLns);
        description.addContent(mmediaContent);
        mmediaContent.addContent(image);
        image.addContent(visualDescriptor);
        visualDescriptor.addContent(histogramm);
        description.setAttribute("type","ContentEntityType");

        visualDescriptor.setAttribute("type","ColorStructureDescriptor");

        for ( int b = 0; b < ColorHistogram.length ; b++ )
        {
            Histogram = Histogram + (int)ColorHistogram[b] + " ";       // build an string with the histogram values in a row
        }
        histogramm.setText(Histogram);
        root.addContent(description);
        Document document = new Document(root);
        try {

           Format format = Format.getPrettyFormat();
           format.setEncoding("iso-8859-1");
           XMLOutputter serializer = new XMLOutputter(format);

           if (FILEOUTPUT)
                {
                    FileOutputStream out = new FileOutputStream(XMLfileName);       // output to XML file
                    serializer.output(document, out);
                }

           return serializer.outputString(document);

        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * With <code>setQuantizationLevels(int)</code> class you can set the quantisation size
     * @param number
     * @throws Exception
     */

    public static void setQuantizationLevels(int number) throws Exception
    {
        if ((number != 32) && (number != 64) && (number != 128) && (number != 256)) throw new Exception("have to be chosen from: 32, 64, 128, 256");
        M = number;
    }

    /**
     * @param XMLfile
     * @throws Exception
     */

    public static void setXMLfileName(String XMLfile) throws Exception
    {
        XMLfileName = XMLfile;
    }
}
