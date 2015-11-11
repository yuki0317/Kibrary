/**
 * 
 */
package anisotime;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * @author kensuke
 * @since 2014/07/08
 * @version 0.0.1
 * 
 *          Document containing only numbers (double value)
 * 
 */
final class NumberDocument extends PlainDocument {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7573995261853190455L;

	/**
	 * 
	 */
	NumberDocument() {
		super();
	}

	// private int currentValue = 0;

	@Override
	public void insertString(int offset, String str, AttributeSet attributes) throws BadLocationException {
		if (str == null)
			return;

		String newValue =null;
		int length = getLength();
		// System.out.println(offset + " " + str + " " + getLength());

		if (length == 0) {
			newValue = str;
		} else {
			String currentContent = getText(0, length);
			StringBuffer currentBuffer = new StringBuffer(currentContent);
			currentBuffer.insert(offset, str);
			newValue = currentBuffer.toString();
		}
		checkInput(newValue, offset);
		super.insertString(offset, str, attributes);

	}

	@Override
	public void remove(int offset, int length) throws BadLocationException {
		int currentLength = getLength();
		String currentContent = getText(0, currentLength);
		String before = currentContent.substring(0, offset);
		String after = currentContent.substring(length + offset, currentLength);
		String newValue = before + after;
		// currentValue = checkInput(newValue, offset);
		checkInput(newValue, offset);
		super.remove(offset, length);
	}

	private static void checkInput(String proposedValue, int offset) throws BadLocationException {
		if (proposedValue.length() > 0) {
			if (proposedValue.equals("+") || proposedValue.equals("-"))
				return;
			try {
				Double.parseDouble(proposedValue);
				return;
			} catch (NumberFormatException e) {
				throw new BadLocationException(proposedValue, offset);
			}
		}
	}

}