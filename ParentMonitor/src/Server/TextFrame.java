package Server;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

//frame that displays a bunch of text
public final class TextFrame extends JDialog {
    
    private final JScrollPane scroll;
    private final JEditorPane editor;
    private final JButton button;
    
    @SuppressWarnings("Convert2Lambda")
    protected TextFrame(JFrame parent, Image iconImage, String title, String info, boolean modal) {
        super(parent, title, modal);

        super.setIconImage(iconImage);
        super.setLocationRelativeTo(parent);
        scroll = new JScrollPane();
        editor = new JEditorPane();
        button = new JButton();

        button.setText("Close");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                dispose();
            }
        });

        GridBagLayout layout = new GridBagLayout();

        layout.columnWidths = new int[]{10, 0, 65, 5, 0};
        layout.rowHeights = new int[]{10, 0, 30, 5, 0};
        layout.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};
        layout.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};

        Container contentPane = super.getContentPane();
        contentPane.setLayout(layout);

        editor.setText(info);
        editor.setEditable(false);
        scroll.setViewportView(editor);

        contentPane.add(scroll, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        contentPane.add(button, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        if (parent != null) {
            super.setBounds(new Rectangle(parent.getX() + parent.getWidth() / 4, parent.getY() + parent.getHeight() / 3, parent.getWidth() / 2, parent.getHeight() / 2));
        }
        else {
            super.setSize(600, 600);
        }
        super.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); //default hide on close
    }

    @SuppressWarnings("Convert2Lambda")
    protected TextFrame(JDialog parent, Image iconImage, String title, String info, boolean modal) {
        super(parent, title, modal);

        super.setIconImage(iconImage);
        super.setLocationRelativeTo(parent);
        scroll = new JScrollPane();
        editor = new JEditorPane();
        button = new JButton();

        button.setText("Close");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                dispose();
            }
        });

        GridBagLayout layout = new GridBagLayout();

        layout.columnWidths = new int[]{10, 0, 65, 5, 0};
        layout.rowHeights = new int[]{10, 0, 30, 5, 0};
        layout.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};
        layout.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};

        Container contentPane = super.getContentPane();
        contentPane.setLayout(layout);

        editor.setText(info);
        editor.setEditable(false);
        scroll.setViewportView(editor);

        contentPane.add(scroll, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        contentPane.add(button, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        if (parent != null) {
            super.setBounds(new Rectangle(parent.getX() + parent.getWidth() / 4, parent.getY() + parent.getHeight() / 3, parent.getWidth() / 2, parent.getHeight() / 2));
        }
        else {
            super.setSize(600, 600);
        }
        super.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE); //default hide on close
    }
    
    public String getText() {
        return editor.getText();
    }

    public void setText(String text) {
        editor.setText(text);
    }
    
    public void addText(String text) {
        String previous = editor.getText();
        editor.setText(previous.isEmpty() ? text : previous + "\n" + text);
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static final void showTextFrame(JFrame parent, Image iconImage, String title, String body, boolean modal) {
        TextFrame frame = new TextFrame(parent, iconImage, title, body, modal);
        frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }
}