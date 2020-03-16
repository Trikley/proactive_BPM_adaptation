package thrlrn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

public class Charts extends ApplicationFrame {

	// from: http://www.java2s.com/Code/Java/Chart/JFreeChartLineChartDemo6.htm
	// Javadoc: http://www.jfree.org/jfreechart/api/javadoc/index.html
	
	 public JFreeChart chart;
	
	 public Charts(final String title, XYDataset dataset, String xLabel, String yLabel, boolean autoRange) {
	        super(title);

	        chart = createChart((XYSeriesCollection)dataset, title, xLabel, yLabel, autoRange);
	        final ChartPanel chartPanel = new ChartPanel(chart);
	        chartPanel.setPreferredSize(new java.awt.Dimension(1024, 800));
	        setContentPane(chartPanel);
	    }

	 public void showChart() {
		 this.pack();
		 RefineryUtilities.centerFrameOnScreen(this);
		 this.setVisible(true);

	 }
	 
    private JFreeChart createChart(final XYSeriesCollection dataset, String title, String xLabel, String yLabel, boolean autoRange) {
	        
        final JFreeChart chart = ChartFactory.createXYLineChart(
	            title,      // chart title
	            xLabel,                      // x axis label
	            yLabel,                      // y axis label
	            dataset,                  // data
	            PlotOrientation.VERTICAL,
	            false,                     // include legend
	            true,                     // tooltips
	            false                     // urls
	        );

	        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
	        chart.setBackgroundPaint(Color.white);
    
	        // get a reference to the plot for further customisation...
	        final XYPlot plot = chart.getXYPlot();
	        plot.setBackgroundPaint(Color.WHITE);
	        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
//	        plot.setDomainGridlinePaint(Color.lightGray);
//	        plot.setRangeGridlinePaint(Color.lightGray);
//	        
	        chart.getTitle().setFont(new Font("Arial", Font.PLAIN, 16));
	        
	        int series = dataset.getSeriesCount();
	        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
	        double minrange = Double.MAX_VALUE;
	        double maxrange = 0;
	        Color col;
	        for(int i = 0; i < series; i++) {
	        	renderer.setSeriesLinesVisible(i, true);
	        	renderer.setSeriesShapesVisible(i, false);
	        	//renderer.setBaseShapesFilled(false);
	        	
	        	// stroked lines
	        	
	        	renderer.setSeriesStroke(i,
	        			new BasicStroke(
	        		        1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
	        		        1.0f, new float[] {20.0f/(1+i), 2.0f}, 0.0f)
	        			);
	        			
	        	renderer.setSeriesPaint(i, Color.black);
	        	
	        	
	        	// stroked lines (selecting spec. curves)
	        	
	        	// highlight DYNAMIC
	        	if(i == 0) 
	        	{
		        	renderer.setSeriesStroke(i,
		        			new BasicStroke(
		        		        5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
		        		        1.0f)
		        			);
		        	renderer.setSeriesPaint(i, Color.black);
	        	}		        		        	
	        	
	        	/*
	        	if(i == 5) 
	        	{
		        	renderer.setSeriesStroke(i,
		        			new BasicStroke(
		        		        1.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
		        		        1.0f, new float[] {1.0f, 3.0f}, 0.0f)
		        			);
		        	renderer.setSeriesPaint(i, Color.black);
	        	}		        		        	
	        	
	        	if(i == 1) 
	        	{
		        	renderer.setSeriesStroke(i,
		        			new BasicStroke(
		        		        0.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
		        		        1.0f, new float[] {3.0f, 3.0f}, 0.0f)
		        			);
		        	renderer.setSeriesPaint(i, Color.black);
	        	}		     
	        	*/
	        	
	        	Rectangle rect = new Rectangle(-1, -1, 2, 2); 
	        	//Dimension dd = new Dimension(); 
	        	//dd.setSize(4, 4); 
	        	//rect.setSize(dd); 
	        	renderer.setSeriesShape(i, rect); 
	        	
	        	
	        	// colour range
	        	/*
	        	int clr = (int)(255*((float)(i+1)/(float)series));
	        	System.out.println(clr);;
	        	col = new Color(clr, clr, clr);
	        	renderer.setSeriesPaint(i, col);
	        	*/
	        		        	
	        	XYSeries s = dataset.getSeries(i);
		        if(minrange > s.getMinY()) minrange = s.getMinY();
	        	if(maxrange < s.getMaxY()) maxrange = s.getMaxY();
	        }
    	        
	        plot.setRenderer(renderer);
	        
	        // Manuel range
	        //minrange = 15000;
	        //maxrange = 55000;
	        
	        // change the auto tick unit selection to integer units only...
	        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
	        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	        //if(autoRange) {
	        	rangeAxis.setTickUnit(new NumberTickUnit(2000));
	        	rangeAxis.setRangeWithMargins(minrange, maxrange);
	        //}
	        // OPTIONAL CUSTOMISATION COMPLETED.
	                
	        return chart;
	        
	    }

    
    public void exportPDF(String filename, int width, int height) throws Exception {
		
    	PdfWriter writer = null;

		Document document = new Document();
		
		writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
		document.open();
				
	 	PdfContentByte cb = writer.getDirectContent(); 
	 	PdfTemplate template = cb.createTemplate(width, height);

	 	Graphics2D graphics2d = new PdfGraphics2D(template, width, height,
				new DefaultFontMapper());
		Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width,
				height/2f);
		chart.draw(graphics2d, rectangle2d);
		
		graphics2d.dispose();
		cb.addTemplate(template, 0, 0);
		
	 	document.close(); 

	 }
    
	
	}
