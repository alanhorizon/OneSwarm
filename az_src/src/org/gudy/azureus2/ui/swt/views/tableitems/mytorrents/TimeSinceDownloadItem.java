/*
 * File    : SecondsDownloadingItem.java
 * Created : 01 febv. 2004
 * By      : TuxPaper
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;


/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class TimeSinceDownloadItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public TimeSinceDownloadItem(String sTableID) {
    super("timesincedownload", ALIGN_TRAIL, POSITION_INVISIBLE, 70, sTableID);
    setRefreshInterval(INTERVAL_LIVE);
    setMinWidthAuto(true);
  }

  public void refresh(TableCell cell) {
    DownloadManager dm = (DownloadManager)cell.getDataSource();
    
    int value = (dm == null) ? -2 : dm.getStats().getTimeSinceLastDataReceivedInSeconds();
    
    if (!cell.setSortValue(value==-1?Integer.MAX_VALUE:value) && cell.isValid())
      return;

    cell.setText(value==-2?"":(value==-1?Constants.INFINITY_STRING:TimeFormatter.format(value)));;
  }
}
