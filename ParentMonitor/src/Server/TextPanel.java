package Server;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class TextPanel extends JPanel {

    private final JScrollPane scroll;
    private final JEditorPane editor;
    private final JTextField field;
    private final JButton button;
    
    private TextSocket textOutput;

    @SuppressWarnings({"CallToThreadStartDuringObjectConstruction", "Convert2Lambda"})
    public TextPanel(TextSocket socket) {
        textOutput = socket;
        
        scroll = new JScrollPane();
        editor = new JEditorPane();
        
        field = new JTextField("Enter Message...");
        field.setEditable(true);
        field.setToolTipText("Enter Message");
        field.addFocusListener(new FocusListener() {

            private boolean beenFocused = false;

            @Override
            public void focusGained(FocusEvent event) {
                if (field.isEditable()) {
                    if (!beenFocused) {
                        field.setText("");
                    }
                    else {
                        beenFocused = true;
                    }
                }
            }

            @Override
            public void focusLost(FocusEvent event) {

            }
        });

        field.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent event) {

            }

            @Override
            public void keyPressed(KeyEvent event) {
                if (textOutput != null) {
                    if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                        String message = field.getText().trim();
                        textOutput.sendText(message); //send message to parent
                        field.setText("");
                        String previousText = editor.getText();
                        editor.setText(previousText.isEmpty() ? "You: " + message : previousText + "\nYou: " + message);
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent event) {

            }
        });

        button = new JButton();
        button.setText("Send Message");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (textOutput != null) {
                    String message = field.getText().trim();
                    textOutput.sendText(message); //send message to parent
                    field.setText("");
                    String previousText = editor.getText();
                    editor.setText(previousText.isEmpty() ? "You: " + message : previousText + "\nYou: " + message);
                }
            }
        });

        editor.setText("");
        editor.setEditable(false);
        scroll.setViewportView(editor);
        
        GridBagLayout layout = new GridBagLayout();

        layout.columnWidths = new int[]{10, 0, 65, 5, 0};
        layout.rowHeights = new int[]{10, 0, 30, 5, 0};
        layout.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};
        layout.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};

        super.setLayout(layout);

        super.add(scroll, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));
        
        super.add(field, new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        super.add(button, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));
    }
    
    @Override
    public void removeAll() {
        super.removeAll();
        textOutput = null; //will be closed by the ParentPanel wrapper
    }

    public void updateChatPanel(String clientName, String fromClient) {
        String previousText = editor.getText();
        editor.setText(previousText.isEmpty() ? clientName + ": " + fromClient : previousText + "\n" + clientName + ": " + fromClient);
    }
}