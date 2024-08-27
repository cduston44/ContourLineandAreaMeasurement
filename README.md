# ContourLineandAreaMeasurement
This is a plugin for AstroImageJ that facilitiates drawing contours and measuring the area of the resulting regions.

It was written by Tyler Nourai,  Dylan Tivnan, and Evan Williams. It is currently (as of August 2024) maintained by Christopher L Duston (christopher.duston@protonmail.com). See the license file in the attached source directory for copyleft infomation.

Installation is done by moving the MyPear_.jar file into your AstroImageJ plugins directory. If you are working on Linux in a default installation, this is /usr/local/AstroImageJ/plugins. Then reopen AstroImageJ, and you will see the menu option Contour Line and Area Measurement under the main "Plugins" menu. To use it:

1. Select a region in AIJ. Rectangles have been most extensively tested, but other shapes should work as well.
2. Select the Contour Line and Area Measurement menu option. Enter values for the minimum and maximum pixel values. These have been automatically set for the region, but in particular the lowest value should probably be increased above the background fluctuations.
3. A new image will appear with the region and colored in contours. The measurement table is now showing the area, in pixels, of each region. This window can be saved as an image, and the table can be saved to a standard spreadsheet format.
