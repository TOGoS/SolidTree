package togos.hdrutil;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class AdjusterUI extends Canvas
{
	HDRImage hdrImage;
	int[] argbBuf;
	BufferedImage bImg;
	
	public void repaint( Graphics g ) {
		if( hdrImage == null ) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
		} else {
			hdrImage.toArgb(argbBuf);
			bImg.setRGB(0, 0, hdrImage.width, hdrImage.height, argbBuf, 0, hdrImage.width);
			g.drawImage(bImg, 0, 0, null);
		}
	}
}
