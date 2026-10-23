package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.commons.swing.AmadeUI;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * The display: indicators (M, DEG, 2nd, brackets) and the expression in a small line, the number large below. A
 * long number gets a smaller font instead of being cut off.
 */
final class Display extends JPanel {
    private final JLabel indicators = new JLabel(" ");
    private final JLabel expression = new JLabel(" ", SwingConstants.RIGHT);
    private final Value value = new Value();

    Display() {
        super(new BorderLayout(0, 2));
        setName("display");
        setOpaque(true);
        AmadeUI.style(this, (panel, theme) -> {
            panel.setBackground(theme.fieldBackground());
            indicators.setForeground(theme.textInactive());
            expression.setForeground(theme.textInactive());
            value.setForeground(theme.fieldText());
        });
        setBorder(BorderFactory.createCompoundBorder(AmadeUI.sunken(), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        indicators.setName("display.indicators");
        indicators.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.round(AmadeUI.scaled(10))));
        expression.setName("display.expression");
        expression.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(AmadeUI.scaled(13))));
        value.setName("display.value");
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(indicators, BorderLayout.WEST);
        top.add(expression, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
    }

    void show(String indicatorText, String expressionText, String valueText, boolean error) {
        indicators.setText(indicatorText.isEmpty() ? " " : indicatorText);
        expression.setText(expressionText.isEmpty() ? " " : expressionText);
        expression.setToolTipText(expressionText.isEmpty() ? null : expressionText);
        value.setText(valueText);
        value.error = error;
        value.repaint();
    }

    String valueText() {
        return value.text;
    }

    String expressionText() {
        return expression.getText().strip();
    }

    String indicatorText() {
        return indicators.getText().strip();
    }

    /** The number, right-aligned; the font shrinks until it fits. */
    private static final class Value extends JLabel {
        private static final float LARGEST = 30f;
        private static final float SMALLEST = 10f;
        private String text = "0";
        private boolean error;

        @Override
        public void setText(String newText) {
            text = newText;
            // The label's own text is what screen readers read; this component paints it itself.
            super.setText(newText);
        }

        private Font font(float size) {
            return new Font(Font.SANS_SERIF, error ? Font.PLAIN : Font.BOLD, Math.round(AmadeUI.scaled(size)));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(font(LARGEST));
            return new Dimension(fm.stringWidth("0000000000"), fm.getAscent() + fm.getDescent());
        }

        // --8<-- [start:fit]
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Insets in = getInsets();
            int width = getWidth() - in.left - in.right;
            float size = error ? LARGEST * 0.6f : LARGEST;
            Font font = font(size);
            FontMetrics fm = g2.getFontMetrics(font);
            while (fm.stringWidth(text) > width && size > SMALLEST) {
                size -= 1;
                font = font(size);
                fm = g2.getFontMetrics(font);
            }
            g2.setFont(font);
            g2.setColor(getForeground());
            int y = in.top + (getHeight() - in.top - in.bottom + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(text, getWidth() - in.right - fm.stringWidth(text), y);
            g2.dispose();
        }
        // --8<-- [end:fit]
    }
}
