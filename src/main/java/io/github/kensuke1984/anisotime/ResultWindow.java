/**
 * 
 */
package io.github.kensuke1984.anisotime;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import io.github.kensuke1984.kibrary.util.Utilities;

/**
 * @author Kensuke Konishi
 * @version 0.0.5.1
 * 
 * 
 */
class ResultWindow extends javax.swing.JPanel {

	private static final long serialVersionUID = -7565301966921737987L;

	/**
	 * Creates new form NewJFrame1
	 */
	public ResultWindow(TravelTimeGUI travelTimeGUI) {
		this.travelTimeGUI = travelTimeGUI;
		initComponents();
	}

	int getN() {
		return jTable1.getRowCount();
	}

	private TravelTimeGUI travelTimeGUI;

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">
	private void initComponents() {
		jScrollPane1 = new JScrollPane();
		jTable1 = new javax.swing.JTable();
		jScrollPane1.setViewportView(jTable1);
		render = new SampleTableCellRenderer02();
		jTable1.setDefaultRenderer(Object.class, render);
		// setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		// jTable1.setEnabled(false);
		jTable1.setDefaultEditor(Object.class, null);
		jTable1.setRowSelectionAllowed(true);
		jTable1.setColumnSelectionAllowed(false);
		jTable1.setShowGrid(true);
		jTable1.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				int row = jTable1.getSelectedRow();
				setColor(row);
				travelTimeGUI.selectRaypath(row);
			}
		});
		jTable1.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {},
				new String[] { "Dist", "Depth", "Name", "Time", "Rayparameter" }));

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE));
		// setPreferredSize(getMinimumSize());
	}// </editor-fold>

	private javax.swing.JScrollPane jScrollPane1;

	void clearRows() {
		((DefaultTableModel) jTable1.getModel()).setRowCount(0);
	}

	/**
	 * @param epicentralDistance
	 *            [deg]
	 * @param depth
	 *            [km]
	 * @param phase
	 *            seismic phase
	 * @param travelTime
	 *            [s]
	 */
	void addRow(double epicentralDistance, double depth, String phase, double travelTime, double rayparameter) {
		String delta = Utilities.fixDecimalPlaces(2, epicentralDistance);
		String depthS = Utilities.fixDecimalPlaces(2, depth);
		String p = Utilities.fixDecimalPlaces(2, rayparameter);
		String time = Utilities.fixDecimalPlaces(2, travelTime);
		try {
			SwingUtilities.invokeAndWait(() -> ((DefaultTableModel) (jTable1.getModel()))
					.addRow(new String[] { delta, depthS, phase, time, p }));

		} catch (Exception e) {
		}

	}

	private SampleTableCellRenderer02 render;

	void setColor(int i) {
		render.featured = i;
		repaint();
	}

	private class SampleTableCellRenderer02 extends DefaultTableCellRenderer {

		private int featured;

		private static final long serialVersionUID = -4100672856859272722L;

		SampleTableCellRenderer02() {
			super();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			/*
			 * どういう呼ばれ方をしてるのか確認するための出力文 目に見えるセルだけ毎度、描画しているらしい スクロールさせると、それがわかる
			 */
			// System.out.println("row:" + row + " /column:" + column +
			// " /selected:" + isSelected + " /focus:" + hasFocus + " /value:" +
			// value);
			if (row == featured) {
				setForeground(Color.RED);
				setBackground(Color.white);
			} else {
				setForeground(Color.BLACK);
				setBackground(Color.white);
			}
			// System.out.println(featured+" fe");
			// // 選択されている行を赤色にする
			// if(isSelected) {
			// this.setBackground(Color.RED);
			// }
			// else {
			// this.setBackground(table.getBackground());
			// }

			// // フォーカスが当たっているセルを黄色にする
			// if(hasFocus) {
			// this.setBackground(Color.yellow);
			// }

			// // 行番号=1/列番号=1のセルを青色にする
			// if((row == 1) && (column == 1)) {
			// this.setBackground(Color.BLUE);
			// }

			return this;
		}
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		// <editor-fold defaultstate="collapsed"
		// desc=" Look and feel setting code (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the
		 * default look and feel. For details see
		 * http://download.oracle.com/javase
		 * /tutorial/uiswing/lookandfeel/plaf.html
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(ResultWindow.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(ResultWindow.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(ResultWindow.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(ResultWindow.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		}
		// </editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(() -> new ResultWindow(null).setVisible(true));
	}

	// Variables declaration - do not modify
	private javax.swing.JTable jTable1;
	// End of variables declaration
}
