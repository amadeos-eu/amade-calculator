package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.apps.calculator.domain.TapeEntry;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.swing.AmadeUI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/** The paper tape beside the keypad: every calculation with its result, right-aligned like on a desk calculator. */
final class PaperTape extends JPanel {
    private final JTextPane text = new JTextPane();
    private final JButton copy;
    private final JButton clear;

    PaperTape(Messages msg) {
        super(new BorderLayout(0, 4));
        setName("tape");
        setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));
        add(new JLabel(msg.get("tape.title")), BorderLayout.NORTH);
        text.setName("tape.text");
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(AmadeUI.scaled(12))));
        SimpleAttributeSet right = new SimpleAttributeSet();
        StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
        text.getStyledDocument().setParagraphAttributes(0, 0, right, false);
        JScrollPane scroll = new JScrollPane(text, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(Math.round(170 * AmadeUI.fontScale()), 100));
        add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
        copy = new JButton(msg.get("tape.copy"));
        copy.setName("tape.copy");
        copy.setFocusable(false);
        clear = new JButton(msg.get("tape.clear"));
        clear.setName("tape.clear");
        clear.setFocusable(false);
        buttons.add(copy);
        buttons.add(clear);
        add(buttons, BorderLayout.SOUTH);
    }

    void onCopy(Runnable action) {
        copy.addActionListener(e -> action.run());
    }

    void onClear(Runnable action) {
        clear.addActionListener(e -> action.run());
    }

    void add(TapeEntry entry) {
        StyledDocument doc = text.getStyledDocument();
        try {
            if (doc.getLength() > 0) doc.insertString(doc.getLength(), "\n\n", null);
            doc.insertString(doc.getLength(), entry.expression() + "\n= " + entry.result(), null);
            SimpleAttributeSet right = new SimpleAttributeSet();
            StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
            doc.setParagraphAttributes(0, doc.getLength(), right, false);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        text.setCaretPosition(doc.getLength());
    }

    void clear() {
        text.setText("");
    }

    String text() {
        return text.getText();
    }
}
