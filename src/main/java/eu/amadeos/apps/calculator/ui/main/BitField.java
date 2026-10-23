package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.theme.DesktopTheme;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigInteger;
import java.util.function.IntConsumer;

/**
 * The 64 bits of the Programmer value in four rows of 16, grouped by four, with the number of the lowest bit of
 * each group below it. A click switches a bit; bits beyond the word size are grey.
 */
final class BitField extends JComponent {
    private static final int ROWS = 4;
    private static final int PER_ROW = 16;
    private BigInteger bits = BigInteger.ZERO;
    private int wordBits = 64;
    private IntConsumer onFlip = bit -> { };

    BitField() {
        setName("bits");
        setBorder(BorderFactory.createCompoundBorder(AmadeUI.sunken(), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int bit = bitAt(e.getX(), e.getY());
                if (bit >= 0 && bit < wordBits) onFlip.accept(bit);
            }
        });
    }

    void onFlip(IntConsumer listener) {
        onFlip = listener;
    }

    void show(BigInteger newBits, int newWordBits) {
        bits = newBits == null ? BigInteger.ZERO : newBits;
        wordBits = newWordBits;
        repaint();
    }

    BigInteger bits() {
        return bits;
    }

    private Font digitFont() {
        return new Font(Font.MONOSPACED, Font.BOLD, Math.round(AmadeUI.scaled(13)));
    }

    private Font numberFont() {
        return new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(AmadeUI.scaled(9)));
    }

    private int cellWidth() {
        return getFontMetrics(digitFont()).charWidth('0');
    }

    private int groupGap() {
        return cellWidth() * 2;
    }

    private int rowHeight() {
        FontMetrics d = getFontMetrics(digitFont());
        FontMetrics n = getFontMetrics(numberFont());
        return d.getHeight() + n.getHeight();
    }

    @Override
    public Dimension getPreferredSize() {
        Insets in = getInsets();
        int width = PER_ROW * cellWidth() + 3 * groupGap();
        return new Dimension(width + in.left + in.right, ROWS * rowHeight() + 2 * (ROWS - 1) + in.top + in.bottom);
    }

    /** Where bit 63 - (row * 16 + column) is drawn. */
    private Rectangle cell(int row, int column) {
        Insets in = getInsets();
        // Centred in the space the layout gives.
        int left = Math.max(in.left, (getWidth() - PER_ROW * cellWidth() - 3 * groupGap()) / 2);
        int top = Math.max(in.top, (getHeight() - ROWS * rowHeight() - 2 * (ROWS - 1)) / 2);
        int x = left + column * cellWidth() + (column / 4) * groupGap();
        int y = top + row * (rowHeight() + 2);
        return new Rectangle(x, y, cellWidth(), getFontMetrics(digitFont()).getHeight());
    }

    int bitAt(int x, int y) {
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < PER_ROW; column++) {
                Rectangle r = cell(row, column);
                r.grow(1, 2);
                if (r.contains(x, y)) return 63 - (row * PER_ROW + column);
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        DesktopTheme theme = AmadeUI.theme();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(theme.fieldBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        FontMetrics digits = g2.getFontMetrics(digitFont());
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < PER_ROW; column++) {
                int bit = 63 - (row * PER_ROW + column);
                Rectangle r = cell(row, column);
                boolean inWord = bit < wordBits;
                boolean set = bits.testBit(bit);
                g2.setFont(digitFont());
                g2.setColor(!inWord ? theme.textInactive() : set ? theme.selectionBackground() : theme.fieldText());
                g2.drawString(set ? "1" : "0", r.x, r.y + digits.getAscent());
                if (bit % 4 == 0) {
                    g2.setFont(numberFont());
                    g2.setColor(theme.textInactive());
                    String number = String.valueOf(bit);
                    FontMetrics n = g2.getFontMetrics();
                    g2.drawString(number, r.x + r.width - n.stringWidth(number), r.y + r.height + n.getAscent());
                }
            }
        }
        g2.dispose();
    }
}
