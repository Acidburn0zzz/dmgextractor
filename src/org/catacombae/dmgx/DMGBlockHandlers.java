/*-
 * Copyright (C) 2006 Erik Larsson
 * 
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.catacombae.dmgx;

import java.io.*;
import java.util.zip.*;

public class DMGBlockHandlers {
    private static byte[] inBuffer = new byte[0x40000];
    private static byte[] outBuffer = new byte[0x40000];
    private static Inflater inflater = new Inflater();

    public static void processZlibBlock(DMGBlock block, RandomAccessFile dmgRaf, RandomAccessFile isoRaf, 
					boolean testOnly, UserInterface ui) throws IOException, DataFormatException {
	inflater.reset();
	
	dmgRaf.seek(/*block.lastOffs+*/block.getInOffset());
	
	/*
	 * medan det finns komprimerat data att l�sa:
	 *   l�s in komprimerat data i inbuffer
	 *   medan det finns data kvar att l�sa i inbuffer
	 *     dekomprimera data fr�n inbuffer till utbuffer
	 *     skriv utbuffer till fil
	 */
	    
	long totalBytesRead = 0;
	while(totalBytesRead < block.getInSize()) {
	    long bytesRemainingToRead = block.getInSize()-totalBytesRead;
	    int curBytesRead = dmgRaf.read(inBuffer, 0, 
					   (int)Math.min(bytesRemainingToRead, inBuffer.length));
		
	    ui.reportProgress((int)(dmgRaf.getFilePointer()*100/dmgRaf.length()));

	    if(curBytesRead < 0)
		throw new RuntimeException("Unexpectedly reached end of file. (bytesRemainingToRead=" + bytesRemainingToRead + ", curBytesRead=" + curBytesRead + ", totalBytesRead=" + totalBytesRead + ", block.getInSize()=" + block.getInSize() + ", inBuffer.length=" + inBuffer.length + ")");
	    else {
		totalBytesRead += curBytesRead;
		inflater.setInput(inBuffer, 0, curBytesRead);
		long totalBytesInflated = 0;
		while(!inflater.needsInput() && !inflater.finished()) {
		    long bytesRemainingToInflate = block.getOutSize()-totalBytesInflated;
		    //System.out.println();
		    //System.out.println("inflater.needsInput()" + inflater.needsInput());
		    int curBytesInflated = inflater.inflate(outBuffer, 0, 
							    (int)Math.min(bytesRemainingToInflate, outBuffer.length));
		    if(curBytesInflated == 0 && !inflater.needsInput()) {
			System.out.println("inflater.finished()" + inflater.finished());
			System.out.println("inflater.needsDictionary()" + inflater.needsDictionary());
			System.out.println("inflater.needsInput()" + inflater.needsInput());
			//System.out.println("inflater.()" + inflater.());
			throw new RuntimeException("Unexpectedly blocked inflate.");
		    }
		    else {
			totalBytesInflated += curBytesInflated;
			if(!testOnly)
			    isoRaf.write(outBuffer, 0, curBytesInflated);
		    }
		}
	    }
	}
	if(!inflater.finished())
	    throw new RuntimeException("Unclosed ZLIB stream!");
    }
}
