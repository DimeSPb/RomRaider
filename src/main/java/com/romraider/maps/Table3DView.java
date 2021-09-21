/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2021 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.maps;

import static com.romraider.util.ParamChecker.isNullOrEmpty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.naming.NameNotFoundException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.romraider.Settings;
import com.romraider.editor.ecu.ECUEditorManager;
import com.romraider.logger.ecu.ui.swing.vertical.VerticalLabelUI;
import com.romraider.util.NumberUtil;
import com.romraider.util.ResourceUtil;
import com.romraider.util.SettingsManager;

public class Table3DView extends TableView {

    private static final long serialVersionUID = 3103448753263606599L;
    private static final ResourceBundle rb = new ResourceUtil().getBundle(
            Table3D.class.getName());
    private Table1D xAxis = new Table1D(TableType.X_AXIS);
    private Table1D yAxis = new Table1D(TableType.Y_AXIS);
    private JLabel xAxisLabel;
    private JLabel yAxisLabel;

    DataCellView[][] data = new DataCellView[1][1];
    private boolean swapXY = false;
    private boolean flipX = false;
    private boolean flipY = false;

    CopyTable3DWorker copyTable3DWorker;
    CopySelection3DWorker copySelection3DWorker;

    public Table3D() {
        verticalOverhead += 39;
        horizontalOverhead += 10;
    }

    @Override
    public TableType getType() {
        return Table.TableType.TABLE_3D;
    }

    public Table1D getXAxis() {
        return xAxis;
    }

    public void setXAxis(Table1D xAxis) {
        this.xAxis = xAxis;
        xAxis.setAxisParent(this);
    }

    public Table1D getYAxis() {
        return yAxis;
    }

    public void setYAxis(Table1D yAxis) {
        this.yAxis = yAxis;
        yAxis.setAxisParent(this);
    }

    public boolean getSwapXY() {
        return swapXY;
    }

    public void setSwapXY(boolean swapXY) {
        this.swapXY = swapXY;
    }

    public boolean getFlipX() {
        return flipX;
    }

    public void setFlipX(boolean flipX) {
        this.flipX = flipX;
    }

    public boolean getFlipY() {
        return flipY;
    }

    public void setFlipY(boolean flipY) {
        this.flipY = flipY;
    }

    public void setSizeX(int size) {
        data = new DataCellView[size][data[0].length];
        centerLayout.setColumns(size + 1);
    }

    public int getSizeX() {
        return data.length;
    }

    public void setSizeY(int size) {
        data = new DataCellView[data.length][size];
        centerLayout.setRows(size + 1);
    }

    public int getSizeY() {
        return data[0].length;
    }

    @Override
    public void drawTable() {
        for(DataCellView[] column : data) {
            for(DataCellView cell : column) {
                if(null != cell) {
                    cell.drawCell();
                }
            }
        }
        xAxis.drawTable();
        yAxis.drawTable();
    }

    @Override
    public void populateTable(byte[] input, int romRamOffset) throws NullPointerException, ArrayIndexOutOfBoundsException, IndexOutOfBoundsException {
    	this.input = input;
    	// fill first empty cell
        centerPanel.add(new JLabel());
        if (!beforeRam) {
            this.ramOffset = romRamOffset;
        }

        // temporarily remove lock
        boolean tempLock = locked;
        locked = false;

        // populate axes
        try {
            xAxis.populateTable(input, romRamOffset);
            yAxis.populateTable(input, romRamOffset);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new ArrayIndexOutOfBoundsException();
        }

        for (int x = 0; x < xAxis.getDataSize(); x++) {
            centerPanel.add(xAxis.getDataCell(x));
        }

        int offset = 0;

        int iMax = swapXY ? xAxis.getDataSize() : yAxis.getDataSize();
        int jMax = swapXY ? yAxis.getDataSize() : xAxis.getDataSize();
        for (int i = 0; i < iMax; i++) {
            for (int j = 0; j < jMax; j++) {

                int x = flipY ? jMax - j - 1 : j;
                int y = flipX ? iMax - i - 1 : i;
                if (swapXY) {
                    int z = x;
                    x = y;
                    y = z;
                }

                // show locked cell
                if (tempLock) {
                    data[x][y].setForeground(Color.GRAY);
                }
                
                DataCell c = new DataCell(this, offset);
                data[x][y] = new DataCellView(c,x,y);
                offset++;
            }
        }

        for (int y = 0; y < yAxis.getDataSize(); y++) {
            centerPanel.add(yAxis.getDataCell(y));
            for (int x = 0; x < xAxis.getDataSize(); x++) {
                centerPanel.add(data[x][y]);
            }
        }

        // reset locked status
        locked = tempLock;

        GridLayout topLayout = new GridLayout(2, 1);
        JPanel topPanel = new JPanel(topLayout);
        this.add(topPanel, BorderLayout.NORTH);
        topPanel.add(new JLabel(getName(), JLabel.CENTER), BorderLayout.NORTH);

        if(null == xAxis.getName() || xAxis.getName().length() < 1 || Settings.BLANK == xAxis.getName()) {
            ;// Do not add label.
        } else if(null == xAxis.getCurrentScale() || "0x" == xAxis.getCurrentScale().getUnit()) {
            // static or no scale exists.
            xAxisLabel = new JLabel(xAxis.getName(), JLabel.CENTER);
            topPanel.add(xAxisLabel, BorderLayout.NORTH);
        } else {
            xAxisLabel = new JLabel(xAxis.getName() + " (" + xAxis.getCurrentScale().getUnit() + ")", JLabel.CENTER);
            topPanel.add(xAxisLabel, BorderLayout.NORTH);
        }

        yAxisLabel = null;
        if(null == yAxis.getName() || yAxis.getName().length() < 1 || Settings.BLANK == yAxis.getName()) {
            ;// Do not add label.
        } else if(null == yAxis.getCurrentScale() || "0x" == yAxis.getCurrentScale().getUnit()) {
            // static or no scale exists.
            yAxisLabel = new JLabel(yAxis.getName());
        } else {
            yAxisLabel = new JLabel(yAxis.getName() + " (" + yAxis.getCurrentScale().getUnit() + ")");
        }

        yAxisLabel.setUI(new VerticalLabelUI(false));
        add(yAxisLabel, BorderLayout.WEST);

        tableLabel = new JLabel(getCurrentScale().getUnit(), JLabel.CENTER);
        add(tableLabel, BorderLayout.SOUTH);
        
        yAxisLabel.setBorder(new EmptyBorder(2, 4, 2, 4));  
        
        if(xAxisLabel!=null)
        	xAxisLabel.setBorder(new EmptyBorder(2, 4, 2, 4)); 
        
        if(presetPanel != null) presetPanel.populatePanel();
        calcCellRanges();
    }

    @Override
    public void updateTableLabel() {
        if(null == xAxis.getName() || xAxis.getName().length() < 1 || Settings.BLANK == xAxis.getName()) {
            ;// Do not update label.
        } else if(null == xAxis.getCurrentScale() || "0x" == xAxis.getCurrentScale().getUnit()) {
            // static or no scale exists.
            xAxisLabel.setText(xAxis.getName());
        } else {
            xAxisLabel.setText(xAxis.getName() + " (" + xAxis.getCurrentScale().getUnit() + ")");
        }

        if(null == yAxis.getName() || yAxis.getName().length() < 1 || Settings.BLANK == yAxis.getName()) {
            ;// Do not update label.
        } else if(null == yAxis.getCurrentScale() || "0x" == yAxis.getCurrentScale().getUnit()) {
            // static or no scale exists.
            yAxisLabel.setText(yAxis.getName());
        } else {
            yAxisLabel.setText(yAxis.getName() + " (" + yAxis.getCurrentScale().getUnit() + ")");
        }

        tableLabel.setText(getCurrentScale().getUnit());
    }

    @Override
    public void calcCellRanges() {
        double binMax = data[0][0].getDataCell().getBinValue();
        double binMin = data[0][0].getDataCell().getBinValue();

        double compareMax = data[0][0].getDataCell().getCompareValue();
        double compareMin = data[0][0].getDataCell().getCompareValue();

        for(DataCellView[] column : data) {
            for(DataCellView cell : column) {
                // Calc bin
                if(binMax < cell.getDataCell().getBinValue()) {
                    binMax = cell.getDataCell().getBinValue();
                }
                if(binMin > cell.getDataCell().getBinValue()) {
                    binMin = cell.getDataCell().getBinValue();
                }

                // Calc compare
                double compareValue = cell.getDataCell().getCompareValue();
                if(compareMax < compareValue) {
                    compareMax = compareValue;
                }
                if(compareMin > compareValue) {
                    compareMin = compareValue;
                }
            }
        }
        setMaxBin(binMax);
        setMinBin(binMin);
        setMaxCompare(compareMax);
        setMinCompare(compareMin);
    }

    @Override
    public StringBuffer getTableAsString() {
        StringBuffer output = new StringBuffer(Settings.BLANK);

        output.append(xAxis.getTableAsString());
        output.append(Settings.NEW_LINE);

        for (int y = 0; y < getSizeY(); y++) {
            output.append(NumberUtil.stringValue(yAxis.data[y].getDataCell().getRealValue()));
            output.append(Settings.TAB);

            for (int x = 0; x < getSizeX(); x++) {
                if (overlayLog) {
                    output.append(data[x][y].getCellText());
                }
                else {
                    output.append(NumberUtil.stringValue(data[x][y].getDataCell().getRealValue()));
                }
                if (x < getSizeX() - 1) {
                    output.append(Settings.TAB);
                }
            }

            if (y < getSizeY() - 1) {
                output.append(Settings.NEW_LINE);
            }
        }

        return output;
    }

    @Override
    public void populateCompareValues(Table otherTable) {
        if(null == otherTable || !(otherTable instanceof Table3D)) {
            return;
        }

        Table3D compareTable3D = (Table3D) otherTable;
        if(data.length != compareTable3D.data.length ||
                data[0].length != compareTable3D.data[0].length ||
                xAxis.getDataSize() != compareTable3D.xAxis.getDataSize() ||
                yAxis.getDataSize() != compareTable3D.yAxis.getDataSize()) {
            return;
        }

        clearLiveDataTrace();

        int x=0;
        for (DataCellView[] column : data) {
            int y = 0;
            for(DataCellView cell : column) {
                cell.getDataCell().setCompareValue(compareTable3D.data[x][y].getDataCell());
                y++;
            }
            x++;
        }

        xAxis.populateCompareValues(compareTable3D.getXAxis());
        yAxis.populateCompareValues(compareTable3D.getYAxis());

        calcCellRanges();
        drawTable();
    }

    @Override
    public void refreshCompare() {
        populateCompareValues(getCompareTable());
        xAxis.refreshCompare();
        yAxis.refreshCompare();
    }

    @Override
    public Dimension getFrameSize() {
        int height = verticalOverhead + cellHeight * data[0].length;
        int width = horizontalOverhead + data.length * cellWidth;
        if (height < minHeight) {
            height = minHeight;
        }
        int minWidth = isLiveDataSupported() ? minWidthOverlay : minWidthNoOverlay;
        if (width < minWidth) {
            width = minWidth;
        }
        return new Dimension(width, height);
    }

    @Override
    public String toString() {
        return super.toString() + " (3D)";/* +
                "\n   Flip X: " + flipX +
                "\n   Size X: " + data.length +
                "\n   Flip Y: " + flipY +
                "\n   Size Y: " + data[0].length +
                "\n   Swap X/Y: " + swapXY +
                xAxis +
                yAxis;*/
    }

    @Override
    public void increment(double increment) {
        if (!locked) {
            for (int x = 0; x < this.getSizeX(); x++) {
                for (int y = 0; y < this.getSizeY(); y++) {
                    if (data[x][y].isSelected()) {
                        data[x][y].getDataCell().increment(increment);
                    }
                }
            }
        }
    }

    @Override
    public void multiply(double factor) {
        if (!locked) {
            for (int x = 0; x < this.getSizeX(); x++) {
                for (int y = 0; y < this.getSizeY(); y++) {
                    if (data[x][y].isSelected()) {
                    	
                    	if(getCurrentScale().getName().equals("Raw Value"))
                    		data[x][y].getDataCell().multiplyRaw(factor);                	
                    	else 
                    		data[x][y].getDataCell().multiply(factor);                            
                    }
                }
            }
        }
    }

    @Override
    public void clearSelection() {
        xAxis.clearSelectedData();
        yAxis.clearSelectedData();
        clearSelectedData();
    }

    @Override
    public void clearSelectedData() {
        for (int x = 0; x < this.getSizeX(); x++) {
            for (int y = 0; y < this.getSizeY(); y++) {
                data[x][y].setSelected(false);
            }
        }
    }

    @Override
    public void highlight(int xCoord, int yCoord) {
        if (highlight) {
            for (int x = 0; x < this.getSizeX(); x++) {
                for (int y = 0; y < this.getSizeY(); y++) {
                    if (((y >= highlightY && y <= yCoord) ||
                            (y <= highlightY && y >= yCoord)) &&
                            ((x >= highlightX && x <= xCoord) ||
                                    (x <= highlightX && x >= xCoord))) {
                        data[x][y].setHighlighted(true);
                    } else {
                        data[x][y].setHighlighted(false);
                    }
                }
            }
        }
    }

    @Override
    public void stopHighlight() {
        highlight = false;
        // loop through, selected and un-highlight
        for (int x = 0; x < this.getSizeX(); x++) {
            for (int y = 0; y < this.getSizeY(); y++) {
                if (data[x][y].isHighlighted()) {
                    data[x][y].setSelected(true);
                    data[x][y].setHighlighted(false);
                }
            }
        }
    }

    @Override
    public void setRevertPoint() {
        for (int x = 0; x < this.getSizeX(); x++) {
            for (int y = 0; y < this.getSizeY(); y++) {
                data[x][y].getDataCell().setRevertPoint();
            }
        }
        yAxis.setRevertPoint();
        xAxis.setRevertPoint();
    }

    @Override
    public void undoAll() {
        clearLiveDataTrace();
        for (int x = 0; x < this.getSizeX(); x++) {
            for (int y = 0; y < this.getSizeY(); y++) {
                data[x][y].getDataCell().undo();
            }
        }
        yAxis.undoAll();
        xAxis.undoAll();
    }

    @Override
    public void undoSelected() {
        clearLiveDataTrace();
        for (int x = 0; x < this.getSizeX(); x++) {
            for (int y = 0; y < this.getSizeY(); y++) {
                if (data[x][y].isSelected()) {
                    data[x][y].getDataCell().undo();
                }
            }
        }
    }


    @Override
    public byte[] saveFile(byte[] binData) {
    	return binData;
    }

    @Override
    public void setRealValue(String realValue) {
        if (!locked && !(userLevel > getSettings().getUserLevel()) ) {
            for(DataCellView[] column : data) {
                for(DataCellView cell : column) {
                    if(cell.isSelected()) {
                        cell.getDataCell().setRealValue(realValue);
                    }
                }
            }
        } else if (userLevel > getSettings().getUserLevel()) {
            JOptionPane.showMessageDialog(this, MessageFormat.format(
                    rb.getString("USERLVLTOLOW"), userLevel),
                    rb.getString("TBLNOTMODIFY"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        xAxis.setRealValue(realValue);
        yAxis.setRealValue(realValue);
    }

    @Override
    public void addKeyListener(KeyListener listener) {
        xAxis.addKeyListener(listener);
        yAxis.addKeyListener(listener);
        for (int x = 0; x < this.getSizeX(); x++) {
            for (int y = 0; y < this.getSizeY(); y++) {
                data[x][y].addKeyListener(listener);
            }
        }
    }

    public void selectCellAt(int y, Table1D axisType) {
        if (axisType.getType() == Table.TableType.Y_AXIS) {
            selectCellAt(0, y);
        } else { // y axis
            selectCellAt(y, 0);
        }
        ECUEditorManager.getECUEditor().getTableToolBar().updateTableToolBar(this);
    }

    public void deSelectCellAt(int x, int y) {
        clearSelection();
        data[x][y].setSelected(false);
        highlightX = x;
        highlightY = y;
    }

    public void selectCellAt(int x, int y) {
        clearSelection();
        data[x][y].setSelected(true);
        highlightX = x;
        highlightY = y;
    }

    public void selectCellAtWithoutClear(int x, int y) {
        data[x][y].setSelected(true);
        highlightX = x;
        highlightY = y;
    }

    @Override
    public void cursorUp() {
        if (highlightY > 0 && data[highlightX][highlightY].isSelected()) {
            selectCellAt(highlightX, highlightY - 1);
        } else if (data[highlightX][highlightY].isSelected()) {
            xAxis.selectCellAt(highlightX);
        } else {
            xAxis.cursorUp();
            yAxis.cursorUp();
        }
    }

    @Override
    public void cursorDown() {
        if (highlightY < getSizeY() - 1 && data[highlightX][highlightY].isSelected()) {
            selectCellAt(highlightX, highlightY + 1);
        } else {
            xAxis.cursorDown();
            yAxis.cursorDown();
        }
    }

    @Override
    public void cursorLeft() {
        if (highlightX > 0 && data[highlightX][highlightY].isSelected()) {
            selectCellAt(highlightX - 1, highlightY);
        } else if (data[highlightX][highlightY].isSelected()) {
            yAxis.selectCellAt(highlightY);
        } else {
            xAxis.cursorLeft();
            yAxis.cursorLeft();
        }
    }

    @Override
    public void cursorRight() {
        if (highlightX < getSizeX() - 1 && data[highlightX][highlightY].isSelected()) {
            selectCellAt(highlightX + 1, highlightY);
        } else {
            xAxis.cursorRight();
            yAxis.cursorRight();
        }
    }

	@Override
	public void shiftCursorUp() {
        if (highlightY > 0 && data[highlightX][highlightY].isSelected()) {
        	selectCellAtWithoutClear(highlightX, highlightY - 1);
        } else if (data[highlightX][highlightY].isSelected()) {
        	data[highlightX][highlightY].setSelected(false);
        	xAxis.selectCellAt(highlightX);
        } else {
        	xAxis.cursorUp();
        	yAxis.shiftCursorUp();
        }
	}

	@Override
	public void shiftCursorDown() {
        if (highlightY < getSizeY() - 1 && data[highlightX][highlightY].isSelected()) {
        	selectCellAtWithoutClear(highlightX, highlightY + 1);
        } else {
            xAxis.shiftCursorDown();
            yAxis.shiftCursorDown();
        }
	}

	@Override
	public void shiftCursorLeft() {
        if (highlightX > 0 && data[highlightX][highlightY].isSelected()) {
        	selectCellAtWithoutClear(highlightX - 1, highlightY);
        } else if (data[highlightX][highlightY].isSelected()) {
            yAxis.selectCellAt(highlightY);
        } else {
            xAxis.shiftCursorLeft();
            yAxis.shiftCursorLeft();
        }
	}

	@Override
	public void shiftCursorRight() {
        if (highlightX < getSizeX() - 1 && data[highlightX][highlightY].isSelected()) {
        	selectCellAtWithoutClear(highlightX + 1, highlightY);
        } else {
            xAxis.shiftCursorRight();
            yAxis.shiftCursorRight();
        }
	}

    @Override
    public void startHighlight(int x, int y) {
        xAxis.clearSelectedData();
        yAxis.clearSelectedData();
        super.startHighlight(x, y);
    }

    @Override
    public void copySelection() {
        Window ancestorWindow = SwingUtilities.getWindowAncestor(this);
        if(null != ancestorWindow) {
            ancestorWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        ECUEditorManager.getECUEditor().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        copySelection3DWorker = new CopySelection3DWorker(this);
        copySelection3DWorker.execute();

    }

    @Override
    public void copyTable() {
        Window ancestorWindow = SwingUtilities.getWindowAncestor(this);
        if(null != ancestorWindow) {
            ancestorWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        ECUEditorManager.getECUEditor().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        copyTable3DWorker = new CopyTable3DWorker(this);
        copyTable3DWorker.execute();
    }

    @Override
    public void paste() {
        StringTokenizer st = new StringTokenizer(Settings.BLANK);
        String input = Settings.BLANK;
        try {
            input = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor);
            st = new StringTokenizer(input, ST_DELIMITER);
        } catch (UnsupportedFlavorException ex) { /* wrong paste type -- do nothing */
        } catch (IOException ex) {
        }

        String pasteType = st.nextToken();

        if ("[Table3D]".equalsIgnoreCase(pasteType)) { // Paste table
            String currentToken = st.nextToken(Settings.NEW_LINE);
            if (currentToken.endsWith("\t")) {
                currentToken = st.nextToken(Settings.NEW_LINE);
            }
            String xAxisValues = "[Table1D]" + Settings.NEW_LINE + currentToken;

            // build y axis and data values
            StringBuffer yAxisValues = new StringBuffer("[Table1D]" + Settings.NEW_LINE + st.nextToken("\t"));
            StringBuffer dataValues = new StringBuffer("[Table3D]" + Settings.NEW_LINE + st.nextToken("\t") + st.nextToken(Settings.NEW_LINE));
            while (st.hasMoreTokens()) {
                yAxisValues.append("\t").append(st.nextToken("\t"));
                dataValues.append(Settings.NEW_LINE).append(st.nextToken("\t")).append(st.nextToken(Settings.NEW_LINE));
            }

            // put x axis in clipboard and paste
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(xAxisValues), null);
            xAxis.paste();
            // put y axis in clipboard and paste
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(String.valueOf(yAxisValues)), null);
            yAxis.paste();
            // put datavalues in clipboard and paste
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(String.valueOf(dataValues)), null);
            pasteValues();
            // reset clipboard
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(input), null);

        } else if ("[Selection3D]".equalsIgnoreCase(pasteType)) { // paste selection
            pasteValues();
        } else if ("[Selection1D]".equalsIgnoreCase(pasteType)) { // paste selection
            xAxis.paste();
            yAxis.paste();
        }
    }

    public void pasteValues() {
        StringTokenizer st = new StringTokenizer(Settings.BLANK);
        try {
            String input = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor);
            st = new StringTokenizer(input, ST_DELIMITER);
        } catch (UnsupportedFlavorException ex) { /* wrong paste type -- do nothing */
        } catch (IOException ex) {
        }

        String pasteType = st.nextToken();

        // figure paste start cell
        int startX = 0;
        int startY = 0;
        // if pasting a table, startX and Y at 0, else highlight is start
        if ("[Selection3D]".equalsIgnoreCase(pasteType)) {
            startX = highlightX;
            startY = highlightY;
        }

        // set values
        for (int y = startY; st.hasMoreTokens() && y < getSizeY(); y++) {
            String checkToken = st.nextToken(Settings.NEW_LINE);
            if (y==startY && checkToken.endsWith("\t")) {
                checkToken = st.nextToken(Settings.NEW_LINE);
            }
            StringTokenizer currentLine = new StringTokenizer(checkToken, ST_DELIMITER);
            for (int x = startX; currentLine.hasMoreTokens() && x < getSizeX(); x++) {
                String currentToken = currentLine.nextToken();

                try {
                    if (!data[x][y].getText().equalsIgnoreCase(currentToken)) {
                        data[x][y].getDataCell().setRealValue(currentToken);
                    }
                } catch (ArrayIndexOutOfBoundsException ex) { /* copied table is larger than current table*/ }
            }
        }
    }

    @Override
    public void verticalInterpolate() {
        int[] coords = { getSizeX(), getSizeY(), 0, 0};
        DataCellView[][] tableData = get3dData();
        DataCellView[] axisData = getYAxis().getData();
        int i, j;
        for (i = 0; i < getSizeX(); ++i) {
            for (j = 0; j < getSizeY(); ++j) {
                if (tableData[i][j].isSelected()) {
                    if (i < coords[0])
                        coords[0] = i;
                    if (i > coords[2])
                        coords[2] = i;
                    if (j < coords[1])
                        coords[1] = j;
                    if (j > coords[3])
                        coords[3] = j;
                }
            }
        }
        if (coords[3] - coords[1] > 1) {
            double x, x1, x2, y1, y2;
            x1 = axisData[coords[1]].getDataCell().getBinValue();
            x2 = axisData[coords[3]].getDataCell().getBinValue();
            for (i = coords[0]; i <= coords[2]; ++i) {
                y1 = tableData[i][coords[1]].getDataCell().getBinValue();
                y2 = tableData[i][coords[3]].getDataCell().getBinValue();
                for (j = coords[1] + 1; j < coords[3]; ++j) {
                    x = axisData[j].getDataCell().getBinValue();
                    tableData[i][j].getDataCell().setBinValue(linearInterpolation(x, x1, x2, y1, y2));
                }
            }
        }
        // Interpolate y axis in case the y axis in selected.
        this.getYAxis().verticalInterpolate();
    }

    @Override
    public void horizontalInterpolate() {
        int[] coords = { getSizeX(), getSizeY(), 0, 0 };
        DataCellView[][] tableData = get3dData();
        DataCellView[] axisData = getXAxis().getData();
        int i, j;
        for (i = 0; i < getSizeX(); ++i) {
            for (j = 0; j < getSizeY(); ++j) {
                if (tableData[i][j].isSelected()) {
                    if (i < coords[0])
                        coords[0] = i;
                    if (i > coords[2])
                        coords[2] = i;
                    if (j < coords[1])
                        coords[1] = j;
                    if (j > coords[3])
                        coords[3] = j;
                }
            }
        }
        if (coords[2] - coords[0] > 1) {
            double x, x1, x2, y1, y2;
            x1 = axisData[coords[0]].getDataCell().getBinValue();
            x2 = axisData[coords[2]].getDataCell().getBinValue();
            for (i = coords[1]; i <= coords[3]; ++i) {
                y1 = tableData[coords[0]][i].getDataCell().getBinValue();
                y2 = tableData[coords[2]][i].getDataCell().getBinValue();
                for (j = coords[0] + 1; j < coords[2]; ++j) {
                    x = axisData[j].getDataCell().getBinValue();
                    tableData[j][i].getDataCell().setBinValue(linearInterpolation(x, x1, x2, y1, y2));
                }
            }
        }
        // Interpolate x axis in case the x axis in selected.
        this.getXAxis().horizontalInterpolate();
    }

    @Override
    public void interpolate() {
        verticalInterpolate();
        horizontalInterpolate();
    }

    @Override
    public boolean isLiveDataSupported() {
        return !isNullOrEmpty(xAxis.getLogParam()) && !isNullOrEmpty(yAxis.getLogParam());
    }

    @Override
    public boolean isButtonSelected() {
        return true;
    }

    @Override
    public void highlightLiveData(String liveValue) {
        if (getOverlayLog()) {
            int x = xAxis.getLiveDataIndex();
            int y = yAxis.getLiveDataIndex();
            DataCellView cell = data[x][y];
            cell.setLiveDataTrace(true);
            cell.getDataCell().setLiveDataTraceValue(liveValue);
            getToolbar().setLiveDataValue(liveValue);
        }
    }

    @Override
    public void updateLiveDataHighlight() {
        if (getOverlayLog()) {
            int x = xAxis.getLiveDataIndex();
            int y = yAxis.getLiveDataIndex();
            int xp = xAxis.getPreviousLiveDataIndex();
            int yp = yAxis.getPreviousLiveDataIndex();
            data[xp][yp].setPreviousLiveDataTrace(true);
            data[x][y].setPreviousLiveDataTrace(false);
            data[x][y].setLiveDataTrace(true);
        }
    }

    @Override
    public void clearLiveDataTrace() {
        xAxis.clearLiveDataTrace();
        yAxis.clearLiveDataTrace();
        for (int x = 0; x < getSizeX(); x++) {
            for (int y = 0; y < getSizeY(); y++) {
                data[x][y].setLiveDataTrace(false);
                data[x][y].setPreviousLiveDataTrace(false);
            }
        }
    }

    public DataCellView[][] get3dData() {
        return data;
    }

    @Override
    public void setCompareDisplay(Settings.CompareDisplay compareDisplay) {
        super.setCompareDisplay(compareDisplay);
        xAxis.setCompareDisplay(compareDisplay);
        yAxis.setCompareDisplay(compareDisplay);
    }

    @Override
    public void setCompareValueType(Settings.DataType compareValueType) {
        super.setCompareValueType(compareValueType);
        xAxis.setCompareValueType(compareValueType);
        yAxis.setCompareValueType(compareValueType);
    }

    @Override
    public void setCurrentScale(Scale curScale) {
        if(SettingsManager.getSettings().isScaleHeadersAndData()) {
            if(!xAxis.isStaticDataTable()) {
                try {
                    this.xAxis.setScaleByName(curScale.getName());
                } catch (NameNotFoundException e) {
                    try {
                        this.xAxis.setScaleByName(SettingsManager.getSettings().getDefaultScale());
                    } catch (NameNotFoundException e1) {
                    }
                }
            }
            if(!yAxis.isStaticDataTable()) {
                try {
                    this.yAxis.setScaleByName(curScale.getName());
                } catch (NameNotFoundException e) {
                    try {
                        this.yAxis.setScaleByName(SettingsManager.getSettings().getDefaultScale());
                    } catch (NameNotFoundException e1) {
                    }
                }
            }
        }
        this.curScale = curScale;
        updateTableLabel();
        drawTable();
    }

    @Override
    public String getLogParamString() {
        StringBuilder sb = new StringBuilder();
        sb.append(xAxis.getLogParamString()+", ");
        sb.append(yAxis.getLogParamString()+", ");
        sb.append(getName()+ ":" + getLogParam());
        return sb.toString();
    }

    @Override
    public void setOverlayLog(boolean overlayLog) {
        super.setOverlayLog(overlayLog);
        xAxis.setOverlayLog(overlayLog);
        yAxis.setOverlayLog(overlayLog);
    }

    @Override
    public boolean equals(Object other) {
        try {
            if(null == other) {
                return false;
            }

            if(other == this) {
                return true;
            }

            if(!(other instanceof Table3D)) {
                return false;
            }

            Table3D otherTable = (Table3D)other;

            if( (null == this.getName() && null == otherTable.getName())
                    || (this.getName().isEmpty() && otherTable.getName().isEmpty()) ) {
                ;// Skip name compare if name is null or empty.
            } else if(!this.getName().equalsIgnoreCase(otherTable.getName())) {
                return false;
            }

            if(! this.xAxis.equals(otherTable.xAxis)) {
                return false;
            }

            if(! this.yAxis.equals(otherTable.yAxis)) {
                return false;
            }

            if(this.data.length != otherTable.data.length || this.data[0].length != otherTable.data[0].length)
            {
                return false;
            }

            if(this.data.equals(otherTable.data))
            {
                return true;
            }

            // Compare Bin Values
            for(int i = 0 ; i < this.data.length ; i++) {
                for(int j = 0; j < this.data[i].length ; j++) {
                    if(! this.data[i][j].equals(otherTable.data[i][j]) ) {
                        return false;
                    }
                }
            }

            return true;
        } catch(Exception ex) {
            // TODO: Log Exception.
            return false;
        }
    }

    @Override
    public void repaint() {
        super.repaint();

        if(null != xAxis) {
            xAxis.repaint();
        }

        if(null != yAxis) {
            yAxis.repaint();
        }
    }
}

class CopySelection3DWorker extends SwingWorker<Void, Void> {
    Table3D table;

    public CopySelection3DWorker(Table3D table)
    {
        this.table = table;
    }

    @Override
    protected Void doInBackground() throws Exception {
        // find bounds of selection
        // coords[0] = x min, y min, x max, y max
        boolean copy = false;
        int[] coords = new int[4];
        coords[0] = table.getSizeX();
        coords[1] = table.getSizeY();

        for (int x = 0; x < table.getSizeX(); x++) {
            for (int y = 0; y < table.getSizeY(); y++) {
                if (table.get3dData()[x][y].isSelected()) {
                    if (x < coords[0]) {
                        coords[0] = x;
                        copy = true;
                    }
                    if (x > coords[2]) {
                        coords[2] = x;
                        copy = true;
                    }
                    if (y < coords[1]) {
                        coords[1] = y;
                        copy = true;
                    }
                    if (y > coords[3]) {
                        coords[3] = y;
                        copy = true;
                    }
                }
            }
        }
        // make string of selection
        if (copy) {
            StringBuffer output = new StringBuffer("[Selection3D]" + Settings.NEW_LINE);
            for (int y = coords[1]; y <= coords[3]; y++) {
                for (int x = coords[0]; x <= coords[2]; x++) {
                    if (table.get3dData()[x][y].isSelected()) {
                        output.append(NumberUtil.stringValue(table.get3dData()[x][y].getDataCell().getRealValue()));
                    } else {
                        output.append("x"); // x represents non-selected cell
                    }
                    if (x < coords[2]) {
                        output.append("\t");
                    }
                }
                if (y < coords[3]) {
                    output.append(Settings.NEW_LINE);
                }
                //copy to clipboard
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(String.valueOf(output)), null);
            }
        } else {
            table.getXAxis().copySelection();
            table.getYAxis().copySelection();
        }
        return null;
    }

    @Override
    public void done() {
        Window ancestorWindow = SwingUtilities.getWindowAncestor(table);
        if(null != ancestorWindow) {
            ancestorWindow.setCursor(null);
        }
        table.setCursor(null);
        ECUEditorManager.getECUEditor().setCursor(null);
    }
}

class CopyTable3DWorker extends SwingWorker<Void, Void> {
    Table3D table;

    public CopyTable3DWorker(Table3D table)
    {
        this.table = table;
    }

    @Override
    protected Void doInBackground() throws Exception {
        String tableHeader = table.getSettings().getTable3DHeader();
        StringBuffer output = new StringBuffer(tableHeader);
        output.append(table.getTableAsString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(String.valueOf(output)), null);
        return null;
    }

    @Override
    public void done() {
        Window ancestorWindow = SwingUtilities.getWindowAncestor(table);
        if(null != ancestorWindow){
            ancestorWindow.setCursor(null);
        }
        table.setCursor(null);
        ECUEditorManager.getECUEditor().setCursor(null);
    }
}
