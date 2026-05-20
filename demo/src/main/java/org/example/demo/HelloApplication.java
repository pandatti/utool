package org.example.demo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class HelloApplication extends Application {
    private BorderPane root;                 // 根布局
    private VBox editorPanel;                // 包含编辑器+工具栏的面板（动态添加/移除）
    private TextArea editorTextArea;
    private File currentEditFile;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();

        // 创建菜单栏（始终显示）
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // 初始状态：中心显示空白（一个浅灰标签，无文字）
        Label blankLabel = new Label();
        blankLabel.setStyle("-fx-background-color: #f0f0f0;");
        root.setCenter(blankLabel);

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("知识管理系统");
        stage.setScene(scene);
        stage.show();
    }

    // 恢复到首页空白状态
    private void resetToBlank() {
        Label blankLabel = new Label();
        blankLabel.setStyle("-fx-background-color: #f0f0f0;");
        root.setCenter(blankLabel);
        editorPanel = null;          // 清除引用
        editorTextArea = null;
        currentEditFile = null;
        statusLabel = null;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu menuFile = new Menu("文件");
        Menu menuEdit = new Menu("编辑");
        Menu menuWorkspace = new Menu("工作空间");
        Menu menuPersonal = new Menu("个人空间");
        Menu menuKnowledge = new Menu("知识库");
        Menu menuHelp = new Menu("帮助");

        // 文件菜单
        MenuItem saveItem = new MenuItem("保存当前内容");
        saveItem.setOnAction(e -> saveCurrentFile());
        MenuItem exitItem = new MenuItem("退出");
        exitItem.setOnAction(e -> System.exit(0));
        menuFile.getItems().addAll(saveItem, new SeparatorMenuItem(), exitItem);

        // 编辑菜单（查找/替换/删除行/清空）
        MenuItem findItem = new MenuItem("查找文本");
        findItem.setOnAction(e -> findText());
        MenuItem replaceItem = new MenuItem("替换文本");
        replaceItem.setOnAction(e -> replaceText());
        MenuItem deleteLineItem = new MenuItem("删除当前行");
        deleteLineItem.setOnAction(e -> deleteCurrentLine());
        MenuItem clearAllItem = new MenuItem("清空所有内容");
        clearAllItem.setOnAction(e -> clearAllContent());
        menuEdit.getItems().addAll(findItem, replaceItem, deleteLineItem, clearAllItem);

        // 工作空间 -> 业财税
        Menu ecsMenu = new Menu("业财税");

        MenuItem requirementItem = new MenuItem("需求文件");
        requirementItem.setOnAction(e -> openDirectory("E:\\zzhongyang\\Kinlong Project\\ECS\\需求文件"));

        MenuItem logItem = new MenuItem("系统功能更新日志");
        logItem.setOnAction(e -> openLogFileInEditor());

        ecsMenu.getItems().addAll(requirementItem, logItem);
        menuWorkspace.getItems().add(ecsMenu);

        // 工作空间根目录
        MenuItem workspaceRootItem = new MenuItem("打开工作空间根目录");
        workspaceRootItem.setOnAction(e -> openDirectory("E:\\zzhongyang\\Kinlong Project"));
        menuWorkspace.getItems().add(0, workspaceRootItem);

        // 知识库
        MenuItem knowledgeRootItem = new MenuItem("打开知识库根目录");
        knowledgeRootItem.setOnAction(e -> openDirectory("E:\\zzhongyang\\Knowledge"));
        MenuItem economyItem = new MenuItem("经济");
        economyItem.setOnAction(e -> openDirectory("E:\\zzhongyang\\Knowledge\\经济"));
        menuKnowledge.getItems().addAll(knowledgeRootItem, economyItem);

        // 帮助
        MenuItem helpItem = new MenuItem("打开目录索引文件");
        helpItem.setOnAction(e -> openFile("E:\\zzhongyang\\目录索引.txt"));
        menuHelp.getItems().add(helpItem);

        // 个人空间占位
        MenuItem personalItem = new MenuItem("个人空间（暂无功能）");
        personalItem.setOnAction(e -> showInfo("个人空间功能待开发"));
        menuPersonal.getItems().add(personalItem);

        menuBar.getMenus().addAll(menuFile, menuEdit, menuWorkspace, menuPersonal, menuKnowledge, menuHelp);
        return menuBar;
    }

    // 创建编辑器面板（包含 TextArea 和工具栏）
    private VBox createEditorPanel() {
        editorTextArea = new TextArea();
        editorTextArea.setWrapText(true);
        editorTextArea.setPromptText("系统功能更新日志 - 在此编辑内容");
        editorTextArea.setEditable(true);
        VBox.setVgrow(editorTextArea, Priority.ALWAYS);

        // 工具栏左侧按钮样式：缩小一号
        String buttonStyle = "-fx-font-size: 11px; -fx-padding: 4px 8px;";

        // 工具栏左侧功能按钮
        Button saveBtn = new Button("保存");
        saveBtn.setStyle(buttonStyle);
        saveBtn.setOnAction(e -> saveCurrentFile());

        Button findBtn = new Button("查找");
        findBtn.setStyle(buttonStyle);
        findBtn.setOnAction(e -> findText());

        Button replaceBtn = new Button("替换");
        replaceBtn.setStyle(buttonStyle);
        replaceBtn.setOnAction(e -> replaceText());

        Button deleteLineBtn = new Button("删除当前行");
        deleteLineBtn.setStyle(buttonStyle);
        deleteLineBtn.setOnAction(e -> deleteCurrentLine());

        Button clearBtn = new Button("清空全部");
        clearBtn.setStyle(buttonStyle);
        clearBtn.setOnAction(e -> clearAllContent());

        HBox leftButtons = new HBox(10, saveBtn, findBtn, replaceBtn, deleteLineBtn, clearBtn);
        leftButtons.setAlignment(Pos.CENTER_LEFT);

        // 右侧红色关闭按钮
        Button closeBtn = new Button("✖ 关闭");
        closeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-font-weight: bold;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-font-weight: bold;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-font-weight: bold;"));
        closeBtn.setOnAction(e -> resetToBlank());

        HBox toolbar = new HBox();
        toolbar.setPadding(new Insets(10));
        toolbar.setSpacing(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        // 左侧按钮区域
        toolbar.getChildren().add(leftButtons);
        // 占位区域，将关闭按钮推到右侧
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(spacer, closeBtn);

        statusLabel = new Label("就绪 | 未保存");
        statusLabel.setPadding(new Insets(5));
        statusLabel.setStyle("-fx-font-size: 11px;");

        VBox panel = new VBox(toolbar, editorTextArea, statusLabel);
        VBox.setVgrow(editorTextArea, Priority.ALWAYS);
        return panel;
    }

    // 加载系统功能更新日志文件到编辑器，并显示编辑器面板
    private void openLogFileInEditor() {
        // 如果已经显示了编辑器面板，先移除（避免重复添加）
        if (editorPanel != null) {
            root.getChildren().remove(editorPanel);
        }
        // 创建新的编辑器面板
        editorPanel = createEditorPanel();
        root.setCenter(editorPanel);

        String filePath = "E:\\zzhongyang\\Kinlong Project\\ECS\\系统功能更新日志.txt";
        File file = new File(filePath);
        currentEditFile = file;

        // 确保目录存在
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 如果文件不存在则创建空文件
        if (!file.exists()) {
            try {
                file.createNewFile();
                statusLabel.setText("文件不存在，已创建空文件：" + filePath);
                editorTextArea.clear();
            } catch (IOException e) {
                showAlert("无法创建文件：" + e.getMessage());
                return;
            }
        }

        // 读取文件内容
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            editorTextArea.setText(content.toString());
            statusLabel.setText("已加载文件：" + filePath);
        } catch (IOException e) {
            showAlert("读取文件失败：" + e.getMessage());
        }
    }

    // 保存当前编辑器内容到文件
    private void saveCurrentFile() {
        if (currentEditFile == null || editorPanel == null || !root.getCenter().equals(editorPanel)) {
            showAlert("请先通过“系统功能更新日志”加载文件");
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentEditFile), StandardCharsets.UTF_8))) {
            writer.write(editorTextArea.getText());
            statusLabel.setText("已保存到：" + currentEditFile.getAbsolutePath());
            showInfo("保存成功");
        } catch (IOException e) {
            showAlert("保存失败：" + e.getMessage());
        }
    }

    // 查找文本
    private void findText() {
        if (editorTextArea == null || !root.getCenter().equals(editorPanel)) {
            showAlert("没有打开的编辑器");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("查找");
        dialog.setHeaderText("输入要查找的文本");
        dialog.setContentText("查找内容:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(searchText -> {
            String content = editorTextArea.getText();
            int index = content.indexOf(searchText);
            if (index >= 0) {
                editorTextArea.selectRange(index, index + searchText.length());
                editorTextArea.requestFocus();
                statusLabel.setText("找到 \"" + searchText + "\" 在位置 " + index);
            } else {
                showInfo("未找到 \"" + searchText + "\"");
            }
        });
    }

    // 替换文本
    private void replaceText() {
        if (editorTextArea == null || !root.getCenter().equals(editorPanel)) {
            showAlert("没有打开的编辑器");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("替换");
        dialog.setHeaderText("替换文本");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        TextField findField = new TextField();
        TextField replaceField = new TextField();
        grid.add(new Label("查找:"), 0, 0);
        grid.add(findField, 1, 0);
        grid.add(new Label("替换为:"), 0, 1);
        grid.add(replaceField, 1, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String find = findField.getText();
                String replace = replaceField.getText();
                if (find.isEmpty()) return;
                String newText = editorTextArea.getText().replace(find, replace);
                editorTextArea.setText(newText);
                statusLabel.setText("已替换 \"" + find + "\" 为 \"" + replace + "\"");
            }
        });
    }

    // 删除当前行
    private void deleteCurrentLine() {
        if (editorTextArea == null || !root.getCenter().equals(editorPanel)) return;
        String text = editorTextArea.getText();
        int caretPos = editorTextArea.getCaretPosition();
        if (text.isEmpty()) return;
        int lineStart = text.lastIndexOf('\n', caretPos - 1) + 1;
        int lineEnd = text.indexOf('\n', caretPos);
        if (lineEnd == -1) lineEnd = text.length();
        String newText = text.substring(0, lineStart) + text.substring(lineEnd + (lineEnd < text.length() ? 1 : 0));
        editorTextArea.setText(newText);
        editorTextArea.positionCaret(Math.min(lineStart, newText.length()));
        statusLabel.setText("已删除当前行");
    }

    // 清空所有内容
    private void clearAllContent() {
        if (editorTextArea == null || !root.getCenter().equals(editorPanel)) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定要清空所有内容吗？", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                editorTextArea.clear();
                statusLabel.setText("已清空内容");
            }
        });
    }

    // 打开目录（外部）
    private void openDirectory(String path) {
        try {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                Desktop.getDesktop().open(dir);
            } else {
                showAlert("目录不存在或无效：" + path);
            }
        } catch (IOException e) {
            showAlert("无法打开目录：" + e.getMessage());
        }
    }

    // 打开文件（外部）
    private void openFile(String path) {
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                Desktop.getDesktop().open(file);
            } else {
                showAlert("文件不存在：" + path);
            }
        } catch (IOException e) {
            showAlert("无法打开文件：" + e.getMessage());
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}