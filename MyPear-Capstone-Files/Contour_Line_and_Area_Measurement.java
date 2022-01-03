/*
 * File: Contour_Line_and_Area_Measurement.java
 *
 * Description: Plugin for the creation of contour lines and
 *              area measurement for the AstroImageJ software.
 *
 *
 * Copyright (C) 2021 MyPear
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * The GNU License version 3 file gpl-3.0.txt included
 * with this software.
 *
 */

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.geom.*;
import java.util.*;
import java.util.ArrayList;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.measure.ResultsTable;
import ij.measure.*;

// Contour lines and area measurement plugin.
public class Contour_Line_and_Area_Measurement implements PlugIn {

	// Reference https://stackoverflow.com/questions/13605248/java-converting-image-to-bufferedimage
	public static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);

		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		return bimage;
	}

    // Method to find the maximum brightness value in the selected region.
	public static double findMaxBrightness(ImagePlus imp, Rectangle roi) {
		ImageProcessor ip = imp.getProcessor();

		float max = 0;

		for (int x = 0; x < roi.getWidth(); x++) {
			for (int y = 0; y < roi.getHeight(); y++) {
				float value = ip.getPixelValue(x + roi.getLocation().x, y + roi.getLocation().y);
				if (max < value) {
					max = value;
				}
			}
		}

		return (double) max;
	}

    // Method to find the minimum brightness value in the selected region.
	public static double findMinBrightness(ImagePlus imp, Rectangle roi) {
		ImageProcessor ip = imp.getProcessor();

		float min = Float.MAX_VALUE;

		for (int x = 0; x < roi.getWidth(); x++) {
			for (int y = 0; y < roi.getHeight(); y++) {
				float value = ip.getPixelValue(x + roi.getLocation().x, y + roi.getLocation().y);
				if (min > value) {
					min = value;
				}
			}
		}

		return (double) min;
	}

	// Method to find the default minimum brightness value for the GUI.
	public static double defaultMinBrightness(double minBrightness, double maxBrightness) {
		double defaultminBrightness = 850;

		if (maxBrightness <= 1000.0) {
                    		defaultminBrightness = 650;
                	}
                	else if (maxBrightness <= 800.0) {
                    		defaultminBrightness = 450;
                	}
                	else if (maxBrightness <= 600.0) {
                	    	defaultminBrightness = 250;
                	}
                	else if (maxBrightness <= 400.0) {
                    		defaultminBrightness = 0;
                	}

		if (defaultminBrightness < minBrightness) {
			defaultminBrightness = minBrightness;
		}

		return defaultminBrightness;
	}

    // Method to find area in the region.
	public static ArrayList<Point> findRegion(ImagePlus imp, int valueLow, int valueHigh, Rectangle roi) {
		ImageProcessor ip = imp.getProcessor();
		float value;
		ArrayList<Point> region = new ArrayList<Point>();

		for (int x = 0; x < roi.getWidth(); x++) {
			for (int y = 0; y < roi.getHeight(); y++) {
				value = ip.getPixelValue(x + roi.getLocation().x, y + roi.getLocation().y);

				if (value > valueLow && value < valueHigh) {
					region.add(new Point(x + roi.getLocation().x, y + roi.getLocation().y));
				}
			}
		}

		return region;
	}

    // Method to find total area in all the regions.
	public static ArrayList<ArrayList<Point>> findAllRegions(ImagePlus imp, int valueLow, int valueHigh, int regions,
			Rectangle roi) {
		ArrayList<ArrayList<Point>> total = new ArrayList<ArrayList<Point>>();
		int incrementor = ((valueHigh - valueLow) / regions) + 1;
		int curValueLow = valueLow;
		int curValueHigh = valueLow + incrementor;

		// regions
		for (int i = 0; i < regions; i++) {
			total.add(findRegion(imp, curValueLow, curValueHigh, roi));

			curValueLow += incrementor;
			curValueHigh += incrementor;
		}

		return total;
	}

    // Method to get the colors of the regions.
	public static Color[] getRegionColors(int valueLow, int valueHigh, int regions) {
		// h for HSB format
		float h = (float) 0.8;
		float incrementor = h / regions;
		Color[] colors = new Color[regions];

		for (int i = regions - 1; i >= 0; i--) {
			colors[i] = new Color(Color.HSBtoRGB(h, 1, 1));
			h -= incrementor;
		}

		return colors;
	}

    // Method to color the regions.
	public static void colorRegions(ImagePlus imp, BufferedImage img, ArrayList<ArrayList<Point>> total, int regions,
			int valueLow, int valueHigh, Rectangle roi) {

		// Obtain the gradient.
		Color[] colors = getRegionColors(valueLow, valueHigh, regions);

		// Make a new image because the ones we have dont want color.
		BufferedImage coloredImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = coloredImage.getGraphics();
		g.drawImage(img, 0, 0, null);

		// Set the color.
		for (int i = 0; i < total.size(); i++) {
			for (int j = 0; j < total.get(i).size(); j++) {
				coloredImage.setRGB(total.get(i).get(j).x, total.get(i).get(j).y, colors[i].getRGB());
			}
		}

		// Set the image color and show it.
		ImagePlus coloredImagePlus = new ImagePlus("Title", coloredImage);
		coloredImagePlus.show();

		GeneralPath path = createContourLines(imp, regions, valueLow, valueHigh, roi);
		Roi roiLine = new ShapeRoi(path);
		roiLine.setStrokeColor(Color.red);
		roiLine.setStrokeWidth(0.2);
		coloredImagePlus.setOverlay(new Overlay(roiLine));
	}

    // Method to create a grid in the region.
	public static int[][] createGrid(ImagePlus imp, Rectangle roi, int valueSeperator) {
		ImageProcessor ip = imp.getProcessor();
		float value;
		int[][] grid;

		// Grid must be even to accomidate 2x2 sets.
		if (roi.getWidth() % 2 == 1) {
			grid = new int[(int) roi.getWidth() - 1][(int) roi.getHeight() - 1];
		} else {
			grid = new int[(int) roi.getWidth()][(int) roi.getHeight()];
		}

		for (int x = 0; x < roi.getWidth() - 1; x++) {
			for (int y = 0; y < roi.getHeight() - 1; y++) {
				value = ip.getPixelValue(x + roi.getLocation().x, y + roi.getLocation().y);

				if (value > valueSeperator) {
					grid[x][y] = 1;
				} else {
					grid[x][y] = 0;
				}
			}
		}

		return grid;
	}

    // Method to draw a line from two points.
	public static GeneralPath drawLine(GeneralPath path, int lineCase, int x, int y, Rectangle roi) {
		// 1 2
		// 4 8
		// Path always draws from top left corner of a pixel.
		switch (lineCase) {
		// all 0
		case 0:
			// dont draw
			break;
		// Bottom Left is 1
		case 1:
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 1.5);
			path.lineTo(roi.getLocation().x + x + 0.5, roi.getLocation().y + y + 1);
			break;
		// Bottom Right is 1
		case 2:
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 1.5);
			path.lineTo(roi.getLocation().x + x + 1.5, roi.getLocation().y + y + 1);
			break;
		// All Bottom is 1
		case 3:
			path.moveTo(roi.getLocation().x + x + 0.5, roi.getLocation().y + y + 1);
			path.lineTo(roi.getLocation().x + x + 1.5, roi.getLocation().y + y + 1);
			break;
		// Top Right is 1
		case 4:
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 0.5);
			path.lineTo(roi.getLocation().x + x + 1.5, roi.getLocation().y + y + 1);
			break;
		// Top Right & Bottom Left is 1
		case 5:
			// case 2 + case 7
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 1.5);
			path.lineTo(roi.getLocation().x + x + 1.5, roi.getLocation().y + y + 1);
			path.moveTo(roi.getLocation().x + x + 0.5, roi.getLocation().y + y + 1);
			path.lineTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 0.5);
			break;
		// All Right is 1
		case 6:
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 0.5);
			path.lineTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 1.5);
			break;
		// All but Top Left is 1
		case 7:
			path.moveTo(roi.getLocation().x + x + 0.5, roi.getLocation().y + y + 1);
			path.lineTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 0.5);
			break;
		// Top Left is 1
		case 8:
			// case 7
			path.moveTo(roi.getLocation().x + x + 0.5, roi.getLocation().y + y + 1);
			path.lineTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 0.5);
			break;
		// All Left is 1
		case 9:
			// case 6
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 0.5);
			path.lineTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 1.5);
			break;
		// Top Left & Bottom Right is 1
		case 10:
			// case 1 + case 4
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 1.5);
			path.lineTo(roi.getLocation().x + x + 0.5, roi.getLocation().y + y + 1);
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 0.5);
			path.lineTo(roi.getLocation().x + x + 1.5, roi.getLocation().y + y + 1);
			break;
		// All but Top Left is 1
		case 11:
			// case 4
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 0.5);
			path.lineTo(roi.getLocation().x + x + 1.5, roi.getLocation().y + y + 1);
			break;
		// All Top is 1
		case 12:
			// case 3
			path.moveTo(roi.getLocation().x + x + 0.5, roi.getLocation().y + y + 1);
			path.lineTo(roi.getLocation().x + x + 1.5, roi.getLocation().y + y + 1);
			break;
		// All but Bottom Right is 1
		case 13:
			// case 2
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 1.5);
			path.lineTo(roi.getLocation().x + x + 1.5, roi.getLocation().y + y + 1);
			break;
		// All but Bottom Left is 1
		case 14:
			// case 1
			path.moveTo(roi.getLocation().x + x + 1, roi.getLocation().y + y + 1.5);
			path.lineTo(roi.getLocation().x + x + 0.5, roi.getLocation().y + y + 1);
			break;
		// All is 1
		case 15:
			// dont draw
			break;
		default:
			// ???
			break;
		}

		return path;
	}

    // Method to create the contour lines.
	public static GeneralPath createContourLines(ImagePlus imp, int regions, int valueLow, int valueHigh,
			Rectangle roi) {
		GeneralPath path = new GeneralPath();
		int incrementor = (valueHigh - valueLow) / regions;
		int seperator = valueLow;
		int[][] grid;

		for (int i = 0; i < regions; i++) {
			grid = createGrid(imp, roi, seperator);

			for (int x = 0; x < roi.getWidth() - 2; x++) {
				for (int y = 0; y < roi.getHeight() - 2; y++) {
					int lineCase = 0;
					// Top Left
					if (grid[x][y] == 1) {
						lineCase += 8;
					}
					// Top Right
					if (grid[x + 1][y] == 1) {
						lineCase += 4;
					}
					// Bottom Right
					if (grid[x + 1][y + 1] == 1) {
						lineCase += 2;
					}
					// Bottom Left
					if (grid[x][y + 1] == 1) {
						lineCase += 1;
					}

					path = drawLine(path, lineCase, x, y, roi);
				}
			}
			seperator += incrementor;
		}

		return path;
	}

    // The run method of the plugin.
	public void run(String arg) {
		// get image
		ImagePlus imp = IJ.getImage();
		ImageProcessor ip = imp.getProcessor();
		BufferedImage img = toBufferedImage(imp.getImage());
		Rectangle roi = ip.getRoi();

		// gui value defaults
		double numLines = 10.0;
		double minBrightness = Math.floor(findMinBrightness(imp, roi));
		double maxBrightness = Math.ceil(findMaxBrightness(imp, roi));
		double defaultMinBrightness = Math.floor(defaultMinBrightness(minBrightness, maxBrightness));

		// Build the gui dialog with user inputs.
		GenericDialog5 gd = new GenericDialog5("Input for Contour Line and Area Measurement Plugin");
		gd.addMessage("Plugin for the Contour Line and Area Measurement\n");
		gd.addSlider("Number of Contour Lines", 1, numLines > 20 ? numLines : 20, numLines);
		gd.addSlider("Minimum Brightness Value", minBrightness,
				minBrightness > maxBrightness ? minBrightness : maxBrightness, defaultMinBrightness);
		gd.addSlider("Maximum Brightness Value", minBrightness,
				maxBrightness > maxBrightness ? maxBrightness : maxBrightness, maxBrightness);
        // Help section.
		gd.addMessage("Notes:");
		gd.addMessage("- A region must be selected before clicking Create.");
		gd.addMessage("- To select a region:");
		gd.addMessage("  1) Click Cancel to exit dialog");
		gd.addMessage("  2) On the image, press and hold the Control key");
		gd.addMessage("  3) Press the left mouse button and drag to draw a rectangle");
		gd.showDialog();

		// get values in order
		int regions = (int) gd.getNextNumber();
		int valueLow = (int) gd.getNextNumber();
		int valueHigh = (int) gd.getNextNumber();

		if (gd.wasCanceled()) {
			// Nothing
		} else if (gd.wasOKed()) {
			// Color image and draw it.
			ArrayList<ArrayList<Point>> total = findAllRegions(imp, valueLow, valueHigh, regions, roi);
			colorRegions(imp, img, total, regions, valueLow, valueHigh, roi);
			// Create results table.
			for (int i=0; i < regions; i++) {
				int theSize =  total.get(i).size();
				IJ.write(String.valueOf(theSize));
			}
		} else {
			// Nothing
		}
	}
}
