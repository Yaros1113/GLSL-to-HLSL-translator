import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class GLSLTranslatorGUI extends JFrame {
    private JTextArea glslInputArea;
    private JTextArea hlslOutputArea;
    private JTextArea logArea;
    private JButton loadButton;
    private JButton translateButton;

    public GLSLTranslatorGUI() {
        setTitle("GLSL to HLSL Translator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Панель для ввода GLSL
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("GLSL Input"));
        glslInputArea = new JTextArea(20, 40);
        glslInputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane inputScroll = new JScrollPane(glslInputArea);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        // Панель для вывода HLSL
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("HLSL Output"));
        hlslOutputArea = new JTextArea(20, 40);
        hlslOutputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        hlslOutputArea.setEditable(false);
        JScrollPane outputScroll = new JScrollPane(hlslOutputArea);
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        // Разделение input и output
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, outputPanel);
        splitPane.setResizeWeight(0.5);
        add(splitPane, BorderLayout.CENTER);

        // Панель для кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout());
        loadButton = new JButton("Load text from file");
        translateButton = new JButton("Translate");
        buttonPanel.add(loadButton);
        buttonPanel.add(translateButton);

        // Панель для логов
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Logs / Errors"));
        logArea = new JTextArea(5, 40);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        // Нижняя панель с кнопками и логами
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(logPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Обработчики событий
        loadButton.addActionListener(new LoadFileAction());
        translateButton.addActionListener(new TranslateAction());

        setVisible(true);
    }

    // Действие для загрузки файла
    private class LoadFileAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(GLSLTranslatorGUI.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    String content = new String(Files.readAllBytes(selectedFile.toPath()));
                    glslInputArea.setText(content);
                    logArea.append("File loaded: " + selectedFile.getName() + "\n");
                } catch (IOException ex) {
                    logArea.append("Error loading file: " + ex.getMessage() + "\n");
                }
            }
        }
    }

    // Действие для трансляции
    private class TranslateAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String glslCode = glslInputArea.getText();
            if (glslCode.trim().isEmpty()) {
                logArea.append("Error: Input GLSL code or load a file.\n");
                return;
            }

            try {
                // Лексер
                GLSLLexer lexer = new GLSLLexer(glslCode);
                List<Token> tokens = lexer.tokenize();
                logArea.append("Lexical analysis finished. Tokens: " + tokens.size() + "\n");

                // Парсер
                GLSLParser parser = new GLSLParser(tokens);
                GLSLParser.Program ast = parser.parseProgram();
                if (!parser.getErrors().isEmpty()) {
                    logArea.append("Parser errors:\n");
                    for (String error : parser.getErrors()) {
                        logArea.append("  " + error + "\n");
                    }
                    return;
                }
                logArea.append("Parsing completed successfully.\n");

                // Генератор HLSL
                HLSLGenerator generator = new HLSLGenerator();
                String hlslCode = generator.generate(ast);
                hlslOutputArea.setText(hlslCode);
                logArea.append("HLSL generation completed.\n");

            } catch (Exception ex) {
                logArea.append("Translation error: " + ex.getMessage() + "\n");
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GLSLTranslatorGUI());
    }
}