package org.example.demo;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelloApplication extends Application {
    private BorderPane root;
    private VBox editorPanel;
    private TextArea editorTextArea;
    private File currentEditFile;
    private Label statusLabel;
    private Node defaultCenterView;
    private Node toolsManagementView;
    private boolean isInToolsManagement = false;

    // 目录常量
    private static final String DEMAND_DIR = "E:\\zzhongyang";
    private static final String TOOL_DATA_DIR = "E:\\zzhongyang\\ToolData";
    private static final String BLANK_DOCX_TEMPLATE = "/blank.docx";

    // MySQL 配置
    private static final String DB_URL = "jdbc:mysql://localhost:3306/knowledge_system?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "zhaoyang@123";  // 请修改为您的数据库密码

    // 书籍数据
    private ObservableList<Book> bookData = FXCollections.observableArrayList();
    private FilteredList<Book> filteredBookData;

    // 密码数据
    private ObservableList<Password> passwordData = FXCollections.observableArrayList();
    private FilteredList<Password> filteredPasswordData;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        defaultCenterView = createDefaultCenterView();
        toolsManagementView = createToolsManagementView();
        root.setCenter(defaultCenterView);
        isInToolsManagement = false;

        Scene scene = new Scene(root, 1000, 700);
        stage.setTitle("知识管理系统");
        stage.setScene(scene);
        stage.show();
    }

    // ------------------------- 密码管理 -------------------------
    private static class Password {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty account;
        private final SimpleStringProperty password;
        private final SimpleStringProperty application;
        private final SimpleStringProperty appType;
        private final SimpleStringProperty remark;

        public Password(int id, String account, String password, String application, String appType, String remark) {
            this.id = new SimpleIntegerProperty(id);
            this.account = new SimpleStringProperty(account);
            this.password = new SimpleStringProperty(password);
            this.application = new SimpleStringProperty(application);
            this.appType = new SimpleStringProperty(appType);
            this.remark = new SimpleStringProperty(remark);
        }

        public int getId() { return id.get(); }
        public SimpleIntegerProperty idProperty() { return id; }
        public String getAccount() { return account.get(); }
        public SimpleStringProperty accountProperty() { return account; }
        public String getPassword() { return password.get(); }
        public SimpleStringProperty passwordProperty() { return password; }
        public String getApplication() { return application.get(); }
        public SimpleStringProperty applicationProperty() { return application; }
        public String getAppType() { return appType.get(); }
        public SimpleStringProperty appTypeProperty() { return appType; }
        public String getRemark() { return remark.get(); }
        public SimpleStringProperty remarkProperty() { return remark; }
    }

    private void loadPasswordsFromDB() {
        passwordData.clear();
        String sql = "SELECT id, account, password, application, app_type, remark FROM passwords";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Password p = new Password(
                        rs.getInt("id"),
                        rs.getString("account"),
                        rs.getString("password"),
                        rs.getString("application"),
                        rs.getString("app_type"),
                        rs.getString("remark")
                );
                passwordData.add(p);
            }
        } catch (SQLException e) {
            showAlert("数据库加载失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addPasswordToDB(Password p) {
        String sql = "INSERT INTO passwords (account, password, application, app_type, remark) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, p.getAccount());
            pstmt.setString(2, p.getPassword());
            pstmt.setString(3, p.getApplication());
            pstmt.setString(4, p.getAppType());
            pstmt.setString(5, p.getRemark());
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    p.id.set(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            showAlert("新增密码失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePasswordInDB(Password p) {
        String sql = "UPDATE passwords SET account=?, password=?, application=?, app_type=?, remark=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getAccount());
            pstmt.setString(2, p.getPassword());
            pstmt.setString(3, p.getApplication());
            pstmt.setString(4, p.getAppType());
            pstmt.setString(5, p.getRemark());
            pstmt.setInt(6, p.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("更新密码失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deletePasswordFromDB(int id) {
        String sql = "DELETE FROM passwords WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("删除密码失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createPasswordManagementView() {
        loadPasswordsFromDB();

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));

        // 查询区
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(5, 0, 10, 0));
        Label searchLabel = new Label("查询：");
        TextField searchAccountField = new TextField();
        searchAccountField.setPromptText("账号");
        TextField searchAppField = new TextField();
        searchAppField.setPromptText("应用");
        Button searchBtn = new Button("搜索");
        Button resetBtn = new Button("重置");
        searchBox.getChildren().addAll(searchLabel, searchAccountField, searchAppField, searchBtn, resetBtn);

        // 表格
        TableView<Password> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Password, Integer> colId = new TableColumn<>("序号");
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colId.setPrefWidth(50);

        TableColumn<Password, String> colAccount = new TableColumn<>("账号");
        colAccount.setCellValueFactory(cellData -> cellData.getValue().accountProperty());

        TableColumn<Password, String> colPassword = new TableColumn<>("密码");
        colPassword.setCellValueFactory(cellData -> cellData.getValue().passwordProperty());

        TableColumn<Password, String> colApplication = new TableColumn<>("应用");
        colApplication.setCellValueFactory(cellData -> cellData.getValue().applicationProperty());

        TableColumn<Password, String> colAppType = new TableColumn<>("应用类型");
        colAppType.setCellValueFactory(cellData -> cellData.getValue().appTypeProperty());

        TableColumn<Password, String> colRemark = new TableColumn<>("备注");
        colRemark.setCellValueFactory(cellData -> cellData.getValue().remarkProperty());

        tableView.getColumns().addAll(colId, colAccount, colPassword, colApplication, colAppType, colRemark);

        filteredPasswordData = new FilteredList<>(passwordData, p -> true);
        tableView.setItems(filteredPasswordData);

        // 搜索过滤
        searchBtn.setOnAction(e -> {
            String accountKw = searchAccountField.getText().trim().toLowerCase();
            String appKw = searchAppField.getText().trim().toLowerCase();
            filteredPasswordData.setPredicate(p -> {
                if (!accountKw.isEmpty() && !p.getAccount().toLowerCase().contains(accountKw)) return false;
                if (!appKw.isEmpty() && !p.getApplication().toLowerCase().contains(appKw)) return false;
                return true;
            });
        });
        resetBtn.setOnAction(e -> {
            searchAccountField.clear();
            searchAppField.clear();
            filteredPasswordData.setPredicate(null);
        });

        // 操作按钮
        HBox actionBar = new HBox(10);
        actionBar.setPadding(new Insets(10, 0, 0, 0));
        Button addBtn = new Button("新增密码");
        Button editBtn = new Button("编辑所选");
        Button delBtn = new Button("删除所选");
        Button backBtn = new Button("◀ 返回首页");
        actionBar.getChildren().addAll(addBtn, editBtn, delBtn, backBtn);

        Runnable refreshData = () -> {
            loadPasswordsFromDB();
            searchAccountField.clear();
            searchAppField.clear();
            filteredPasswordData.setPredicate(null);
        };

        // 双击编辑
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Password selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showPasswordDialog(selected, true);
                    refreshData.run();
                }
            }
        });

        editBtn.setOnAction(e -> {
            Password selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showPasswordDialog(selected, true);
                refreshData.run();
            } else showAlert("请先选择要编辑的密码");
        });

        addBtn.setOnAction(e -> {
            showPasswordDialog(null, false);
            refreshData.run();
        });

        delBtn.setOnAction(e -> {
            Password selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定删除账号：" + selected.getAccount() + " 吗？", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        deletePasswordFromDB(selected.getId());
                        refreshData.run();
                    }
                });
            } else showAlert("请先选择要删除的密码");
        });

        backBtn.setOnAction(e -> resetToBlank());

        container.getChildren().addAll(searchBox, tableView, actionBar);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        return container;
    }

    private void showPasswordDialog(Password password, boolean isEdit) {
        Dialog<Password> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "编辑密码" : "新增密码");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField accountField = new TextField();
        TextField passwordField = new TextField();
        TextField applicationField = new TextField();
        TextField appTypeField = new TextField();
        TextArea remarkArea = new TextArea();
        remarkArea.setPrefRowCount(3);
        remarkArea.setPrefWidth(300);

        if (isEdit && password != null) {
            accountField.setText(password.getAccount());
            passwordField.setText(password.getPassword());
            applicationField.setText(password.getApplication());
            appTypeField.setText(password.getAppType());
            remarkArea.setText(password.getRemark());
        }

        grid.add(new Label("账号:*"), 0, 0);
        grid.add(accountField, 1, 0);
        grid.add(new Label("密码:*"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("应用:"), 0, 2);
        grid.add(applicationField, 1, 2);
        grid.add(new Label("应用类型:"), 0, 3);
        grid.add(appTypeField, 1, 3);
        grid.add(new Label("备注:"), 0, 4);
        grid.add(remarkArea, 1, 4);

        dialog.getDialogPane().setContent(grid);
        ButtonType okType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == okType) {
                String account = accountField.getText().trim();
                String pwd = passwordField.getText().trim();
                if (account.isEmpty() || pwd.isEmpty()) {
                    showAlert("账号和密码不能为空");
                    return null;
                }
                String application = applicationField.getText().trim();
                String appType = appTypeField.getText().trim();
                String remark = remarkArea.getText();
                return new Password(0, account, pwd, application, appType, remark);
            }
            return null;
        });

        Optional<Password> result = dialog.showAndWait();
        result.ifPresent(newPwd -> {
            if (isEdit && password != null) {
                password.account.set(newPwd.getAccount());
                password.password.set(newPwd.getPassword());
                password.application.set(newPwd.getApplication());
                password.appType.set(newPwd.getAppType());
                password.remark.set(newPwd.getRemark());
                updatePasswordInDB(password);
            } else {
                addPasswordToDB(newPwd);
            }
        });
    }

    // ------------------------- 书籍管理（数据库版）-------------------------
    private static class Book {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty bookId;
        private final SimpleStringProperty name;
        private final SimpleStringProperty publisher;
        private final SimpleStringProperty isbn;
        private final SimpleStringProperty author;
        private final SimpleStringProperty purchaseDate;
        private final SimpleStringProperty channel;

        public Book(int id, String bookId, String name, String publisher, String isbn,
                    String author, String purchaseDate, String channel) {
            this.id = new SimpleIntegerProperty(id);
            this.bookId = new SimpleStringProperty(bookId);
            this.name = new SimpleStringProperty(name);
            this.publisher = new SimpleStringProperty(publisher);
            this.isbn = new SimpleStringProperty(isbn);
            this.author = new SimpleStringProperty(author);
            this.purchaseDate = new SimpleStringProperty(purchaseDate);
            this.channel = new SimpleStringProperty(channel);
        }

        public int getId() { return id.get(); }
        public SimpleIntegerProperty idProperty() { return id; }
        public String getBookId() { return bookId.get(); }
        public SimpleStringProperty bookIdProperty() { return bookId; }
        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }
        public String getPublisher() { return publisher.get(); }
        public SimpleStringProperty publisherProperty() { return publisher; }
        public String getIsbn() { return isbn.get(); }
        public SimpleStringProperty isbnProperty() { return isbn; }
        public String getAuthor() { return author.get(); }
        public SimpleStringProperty authorProperty() { return author; }
        public String getPurchaseDate() { return purchaseDate.get(); }
        public SimpleStringProperty purchaseDateProperty() { return purchaseDate; }
        public String getChannel() { return channel.get(); }
        public SimpleStringProperty channelProperty() { return channel; }
    }

    private void loadBooksFromDB() {
        bookData.clear();
        String sql = "SELECT id, book_id, name, publisher, isbn, author, purchase_date, channel FROM books";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Book b = new Book(
                        rs.getInt("id"),
                        rs.getString("book_id"),
                        rs.getString("name"),
                        rs.getString("publisher"),
                        rs.getString("isbn"),
                        rs.getString("author"),
                        rs.getString("purchase_date") != null ? rs.getString("purchase_date") : "",
                        rs.getString("channel")
                );
                bookData.add(b);
            }
        } catch (SQLException e) {
            showAlert("数据库加载失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addBookToDB(Book book) {
        String sql = "INSERT INTO books (book_id, name, publisher, isbn, author, purchase_date, channel) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, book.getBookId());
            pstmt.setString(2, book.getName());
            pstmt.setString(3, book.getPublisher());
            pstmt.setString(4, book.getIsbn());
            pstmt.setString(5, book.getAuthor());
            pstmt.setDate(6, book.getPurchaseDate() != null && !book.getPurchaseDate().isEmpty() ?
                    Date.valueOf(book.getPurchaseDate()) : null);
            pstmt.setString(7, book.getChannel());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("新增书籍失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateBookInDB(Book book) {
        String sql = "UPDATE books SET book_id=?, name=?, publisher=?, isbn=?, author=?, purchase_date=?, channel=? WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getBookId());
            pstmt.setString(2, book.getName());
            pstmt.setString(3, book.getPublisher());
            pstmt.setString(4, book.getIsbn());
            pstmt.setString(5, book.getAuthor());
            pstmt.setDate(6, book.getPurchaseDate() != null && !book.getPurchaseDate().isEmpty() ?
                    Date.valueOf(book.getPurchaseDate()) : null);
            pstmt.setString(7, book.getChannel());
            pstmt.setInt(8, book.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("更新书籍失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteBookFromDB(int id) {
        String sql = "DELETE FROM books WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("删除书籍失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private VBox createBookManagementView() {
        loadBooksFromDB();

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));

        // 查询区
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(5, 0, 10, 0));
        Label searchLabel = new Label("查询：");
        TextField searchNameField = new TextField();
        searchNameField.setPromptText("书籍名称");
        TextField searchAuthorField = new TextField();
        searchAuthorField.setPromptText("作者");
        Button searchBtn = new Button("搜索");
        Button resetBtn = new Button("重置");
        searchBox.getChildren().addAll(searchLabel, searchNameField, searchAuthorField, searchBtn, resetBtn);

        // 表格（使用 Lambda 绑定，避免反射警告）
        TableView<Book> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Book, Integer> colId = new TableColumn<>("序号");
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colId.setPrefWidth(50);

        TableColumn<Book, String> colBookId = new TableColumn<>("书籍编号");
        colBookId.setCellValueFactory(cellData -> cellData.getValue().bookIdProperty());

        TableColumn<Book, String> colName = new TableColumn<>("书籍名称");
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

        TableColumn<Book, String> colPublisher = new TableColumn<>("书籍出版社");
        colPublisher.setCellValueFactory(cellData -> cellData.getValue().publisherProperty());

        TableColumn<Book, String> colIsbn = new TableColumn<>("ISBN");
        colIsbn.setCellValueFactory(cellData -> cellData.getValue().isbnProperty());

        TableColumn<Book, String> colAuthor = new TableColumn<>("作者");
        colAuthor.setCellValueFactory(cellData -> cellData.getValue().authorProperty());

        TableColumn<Book, String> colPurchaseDate = new TableColumn<>("购入时间");
        colPurchaseDate.setCellValueFactory(cellData -> cellData.getValue().purchaseDateProperty());

        TableColumn<Book, String> colChannel = new TableColumn<>("购买渠道");
        colChannel.setCellValueFactory(cellData -> cellData.getValue().channelProperty());

        tableView.getColumns().addAll(colId, colBookId, colName, colPublisher, colIsbn, colAuthor, colPurchaseDate, colChannel);

        filteredBookData = new FilteredList<>(bookData, p -> true);
        tableView.setItems(filteredBookData);

        // 搜索过滤
        searchBtn.setOnAction(e -> {
            String nameKw = searchNameField.getText().trim().toLowerCase();
            String authorKw = searchAuthorField.getText().trim().toLowerCase();
            filteredBookData.setPredicate(book -> {
                if (!nameKw.isEmpty() && !book.getName().toLowerCase().contains(nameKw)) return false;
                if (!authorKw.isEmpty() && !book.getAuthor().toLowerCase().contains(authorKw)) return false;
                return true;
            });
        });
        resetBtn.setOnAction(e -> {
            searchNameField.clear();
            searchAuthorField.clear();
            filteredBookData.setPredicate(null);
        });

        // 操作按钮
        HBox actionBar = new HBox(10);
        actionBar.setPadding(new Insets(10, 0, 0, 0));
        Button addBtn = new Button("新增书籍");
        Button editBtn = new Button("编辑所选");
        Button delBtn = new Button("删除所选");
        Button backBtn = new Button("◀ 返回首页");
        actionBar.getChildren().addAll(addBtn, editBtn, delBtn, backBtn);

        Runnable refreshData = () -> {
            loadBooksFromDB();
            searchNameField.clear();
            searchAuthorField.clear();
            filteredBookData.setPredicate(null);
        };

        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Book selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showBookDialog(selected, true);
                    refreshData.run();
                }
            }
        });

        editBtn.setOnAction(e -> {
            Book selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showBookDialog(selected, true);
                refreshData.run();
            } else showAlert("请先选择要编辑的书籍");
        });

        addBtn.setOnAction(e -> {
            showBookDialog(null, false);
            refreshData.run();
        });

        delBtn.setOnAction(e -> {
            Book selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定删除《" + selected.getName() + "》吗？", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        deleteBookFromDB(selected.getId());
                        refreshData.run();
                    }
                });
            } else showAlert("请先选择要删除的书籍");
        });

        backBtn.setOnAction(e -> resetToBlank());

        container.getChildren().addAll(searchBox, tableView, actionBar);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        return container;
    }

    private void showBookDialog(Book book, boolean isEdit) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "编辑书籍" : "新增书籍");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField bookIdField = new TextField();
        TextField nameField = new TextField();
        TextField publisherField = new TextField();
        TextField isbnField = new TextField();
        TextField authorField = new TextField();
        DatePicker purchaseDatePicker = new DatePicker();
        TextField channelField = new TextField();

        if (isEdit && book != null) {
            bookIdField.setText(book.getBookId());
            nameField.setText(book.getName());
            publisherField.setText(book.getPublisher());
            isbnField.setText(book.getIsbn());
            authorField.setText(book.getAuthor());
            if (book.getPurchaseDate() != null && !book.getPurchaseDate().isEmpty())
                purchaseDatePicker.setValue(LocalDate.parse(book.getPurchaseDate(), DateTimeFormatter.ISO_LOCAL_DATE));
            channelField.setText(book.getChannel());
        }

        grid.add(new Label("书籍编号:"), 0, 0);
        grid.add(bookIdField, 1, 0);
        grid.add(new Label("书籍名称:*"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("出版社:"), 0, 2);
        grid.add(publisherField, 1, 2);
        grid.add(new Label("ISBN:"), 0, 3);
        grid.add(isbnField, 1, 3);
        grid.add(new Label("作者:"), 0, 4);
        grid.add(authorField, 1, 4);
        grid.add(new Label("购入时间:"), 0, 5);
        grid.add(purchaseDatePicker, 1, 5);
        grid.add(new Label("购买渠道:"), 0, 6);
        grid.add(channelField, 1, 6);

        dialog.getDialogPane().setContent(grid);
        ButtonType okType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == okType) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    showAlert("书籍名称不能为空");
                    return null;
                }
                String bookId = bookIdField.getText().trim();
                String publisher = publisherField.getText().trim();
                String isbn = isbnField.getText().trim();
                String author = authorField.getText().trim();
                String purchaseDate = purchaseDatePicker.getValue() != null ? purchaseDatePicker.getValue().toString() : "";
                String channel = channelField.getText().trim();
                return new Book(0, bookId, name, publisher, isbn, author, purchaseDate, channel);
            }
            return null;
        });

        Optional<Book> result = dialog.showAndWait();
        result.ifPresent(newBook -> {
            if (isEdit && book != null) {
                book.bookId.set(newBook.getBookId());
                book.name.set(newBook.getName());
                book.publisher.set(newBook.getPublisher());
                book.isbn.set(newBook.getIsbn());
                book.author.set(newBook.getAuthor());
                book.purchaseDate.set(newBook.getPurchaseDate());
                book.channel.set(newBook.getChannel());
                updateBookInDB(book);
            } else {
                addBookToDB(newBook);
            }
        });
    }

    // ------------------------- 工具管理 -------------------------
    private VBox createToolsManagementView() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setAlignment(Pos.TOP_CENTER);

        GridPane tianGrid = new GridPane();
        tianGrid.setHgap(15);
        tianGrid.setVgap(15);
        tianGrid.setAlignment(Pos.CENTER);
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        RowConstraints row = new RowConstraints();
        row.setPercentHeight(50);
        tianGrid.getColumnConstraints().addAll(col, col);
        tianGrid.getRowConstraints().addAll(row, row);

        ToolCardItem[] cards = {
                new ToolCardItem("密码管理", "🔐", "password"),
                new ToolCardItem("书籍管理", "📚", "book"),
                new ToolCardItem("其他工具", "🔧", "other"),
                new ToolCardItem("待开发", "⏳", "future")
        };

        int index = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                ToolCardItem card = cards[index++];
                VBox cardBox = createToolCard(card.name, card.icon, card.type);
                tianGrid.add(cardBox, j, i);
            }
        }

        Button backHomeBtn = new Button("◀ 返回首页");
        backHomeBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6px 12px;");
        backHomeBtn.setOnAction(e -> resetToBlank());

        HBox bottomBar = new HBox(backHomeBtn);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        container.getChildren().addAll(tianGrid, bottomBar);
        VBox.setVgrow(tianGrid, Priority.ALWAYS);
        return container;
    }

    private static class ToolCardItem {
        String name, icon, type;
        ToolCardItem(String name, String icon, String type) {
            this.name = name; this.icon = icon; this.type = type;
        }
    }

    private VBox createToolCard(String title, String icon, String type) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPrefSize(200, 150);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #4CAF50; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"));
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 36px;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        card.getChildren().addAll(iconLabel, titleLabel);
        card.setOnMouseClicked(e -> {
            switch (type) {
                case "password": openPasswordManager(); break;
                case "book": openBookManager(); break;
                default: showInfo("该功能暂未开放");
            }
        });
        return card;
    }

    private void openPasswordManager() {
        VBox passwordView = createPasswordManagementView();
        root.setCenter(passwordView);
        isInToolsManagement = false;
    }

    private void openBookManager() {
        VBox bookView = createBookManagementView();
        root.setCenter(bookView);
        isInToolsManagement = false;
    }

    // ------------------------- 文本编辑器（原密码管理已移除，保留供日志使用） -------------------------
    private void openTextFileForEditing(File file, String toolName) {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { showAlert("无法创建文件：" + e.getMessage()); return; }
        }
        if (editorPanel == null) editorPanel = createEditorPanel();
        root.setCenter(editorPanel);
        currentEditFile = file;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) content.append(line).append("\n");
            editorTextArea.setText(content.toString());
            statusLabel.setText("已打开 " + toolName + " 文件：" + file.getAbsolutePath());
        } catch (IOException e) { showAlert("读取文件失败：" + e.getMessage()); }
    }

    // ------------------------- 默认首页 -------------------------
    private VBox createDefaultCenterView() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setAlignment(Pos.TOP_CENTER);
        HBox quickAccessBar = createQuickAccessBar();
        quickAccessBar.setPadding(new Insets(0, 0, 10, 0));
        GridPane tianGrid = new GridPane();
        tianGrid.setHgap(15); tianGrid.setVgap(15); tianGrid.setAlignment(Pos.CENTER);
        ColumnConstraints col = new ColumnConstraints(); col.setPercentWidth(50);
        RowConstraints row = new RowConstraints(); row.setPercentHeight(50);
        tianGrid.getColumnConstraints().addAll(col, col); tianGrid.getRowConstraints().addAll(row, row);
        CardItem[] cards = {
                new CardItem("淘宝网", "https://www.taobao.com", true),
                new CardItem("京东网", "https://www.jd.com", true),
                new CardItem("我的钢铁网", "https://www.mysteel.com", true),
                new CardItem("目录索引", "E:\\zzhongyang\\目录索引.txt", false)
        };
        int idx = 0;
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) {
            CardItem card = cards[idx++];
            VBox cardBox = createCard(card.name, card.target, card.isUrl);
            tianGrid.add(cardBox, j, i);
        }
        HBox jumpBar = new HBox(10);
        jumpBar.setAlignment(Pos.CENTER);
        jumpBar.setPadding(new Insets(10, 0, 0, 0));
        TextField inputField = new TextField();
        inputField.setPromptText("输入网址或本地文件/文件夹路径");
        inputField.setPrefWidth(500);
        Button goButton = new Button("GO");
        goButton.setStyle("-fx-font-weight: bold; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        goButton.setOnAction(e -> { String input = inputField.getText().trim(); if (!input.isEmpty()) jumpToTarget(input); });
        jumpBar.getChildren().addAll(inputField, goButton);
        container.getChildren().addAll(quickAccessBar, tianGrid, jumpBar);
        VBox.setVgrow(tianGrid, Priority.ALWAYS);
        return container;
    }

    private static class CardItem {
        String name; String target; boolean isUrl;
        CardItem(String name, String target, boolean isUrl) {
            this.name = name; this.target = target; this.isUrl = isUrl;
        }
    }

    private VBox createCard(String title, String target, boolean isUrl) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPrefSize(200, 150);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #4CAF50; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 3);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"));
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label iconLabel = new Label(isUrl ? "🌐" : "📄");
        iconLabel.setStyle("-fx-font-size: 32px;");
        card.getChildren().addAll(iconLabel, titleLabel);
        card.setOnMouseClicked(e -> {
            if (isUrl) openWebpage(target);
            else openFileOrDir(target);
        });
        return card;
    }

    private VBox createEditorPanel() {
        editorTextArea = new TextArea();
        editorTextArea.setWrapText(true);
        editorTextArea.setPromptText("在此编辑内容...（支持快捷键：Ctrl+S保存、Ctrl+F查找、Ctrl+H替换、Ctrl+D删除行、Ctrl+Shift+C清空）");
        editorTextArea.setEditable(true);
        VBox.setVgrow(editorTextArea, Priority.ALWAYS);
        editorTextArea.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.S) { saveCurrentFile(); event.consume(); }
                else if (event.getCode() == KeyCode.F) { findText(); event.consume(); }
                else if (event.getCode() == KeyCode.H) { replaceText(); event.consume(); }
                else if (event.getCode() == KeyCode.D) { deleteCurrentLine(); event.consume(); }
                else if (event.isShiftDown() && event.getCode() == KeyCode.C) { clearAllContent(); event.consume(); }
            }
        });
        Button closeBtn = new Button("✖ 关闭");
        closeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-font-weight: bold;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-font-weight: bold;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4px 8px; -fx-font-weight: bold;"));
        closeBtn.setOnAction(e -> resetToBlank());
        HBox toolbar = new HBox();
        toolbar.setPadding(new Insets(10));
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        toolbar.getChildren().add(closeBtn);
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(5));
        statusLabel.setStyle("-fx-font-size: 11px;");
        VBox panel = new VBox(toolbar, editorTextArea, statusLabel);
        VBox.setVgrow(editorTextArea, Priority.ALWAYS);
        return panel;
    }

    private void saveCurrentFile() {
        if (currentEditFile == null || editorPanel == null || !root.getCenter().equals(editorPanel)) {
            showAlert("没有打开的编辑器文件");
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

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("文件");
        Menu menuEdit = new Menu("编辑");
        Menu menuWorkspace = new Menu("工作空间");
        Menu menuTools = new Menu("工具管理");
        Menu menuKnowledge = new Menu("知识库");
        Menu menuHelp = new Menu("帮助");

        MenuItem saveItem = new MenuItem("保存当前内容");
        saveItem.setOnAction(e -> saveCurrentFile());
        MenuItem exitItem = new MenuItem("退出");
        exitItem.setOnAction(e -> System.exit(0));
        menuFile.getItems().addAll(saveItem, new SeparatorMenuItem(), exitItem);

        MenuItem findItem = new MenuItem("查找文本");
        findItem.setOnAction(e -> findText());
        MenuItem replaceItem = new MenuItem("替换文本");
        replaceItem.setOnAction(e -> replaceText());
        MenuItem deleteLineItem = new MenuItem("删除当前行");
        deleteLineItem.setOnAction(e -> deleteCurrentLine());
        MenuItem clearAllItem = new MenuItem("清空所有内容");
        clearAllItem.setOnAction(e -> clearAllContent());
        menuEdit.getItems().addAll(findItem, replaceItem, deleteLineItem, clearAllItem);

        MenuItem passwordItem = new MenuItem("密码管理");
        passwordItem.setOnAction(e -> openPasswordManager());
        MenuItem bookItem = new MenuItem("书籍管理");
        bookItem.setOnAction(e -> openBookManager());
        menuTools.getItems().addAll(passwordItem, bookItem);

        menuTools.setOnShowing(event -> {
            if (!isInToolsManagement) {
                root.setCenter(toolsManagementView);
                isInToolsManagement = true;
                editorPanel = null;
                editorTextArea = null;
                currentEditFile = null;
                statusLabel = null;
            }
        });

        Menu ecsMenu = new Menu("业财税");
        MenuItem requirementItem = new MenuItem("需求文件");
        requirementItem.setOnAction(e -> openDirectory("E:\\zzhongyang\\Kinlong Project\\ECS\\需求文件"));
        MenuItem logItem = new MenuItem("系统功能更新日志");
        logItem.setOnAction(e -> openLogFileInEditor());
        ecsMenu.getItems().addAll(requirementItem, logItem);
        menuWorkspace.getItems().add(ecsMenu);
        MenuItem workspaceRootItem = new MenuItem("打开工作空间根目录");
        workspaceRootItem.setOnAction(e -> openDirectory("E:\\zzhongyang\\Kinlong Project"));
        menuWorkspace.getItems().add(0, workspaceRootItem);

        MenuItem knowledgeRootItem = new MenuItem("打开知识库根目录");
        knowledgeRootItem.setOnAction(e -> openDirectory("E:\\zzhongyang\\Knowledge"));
        MenuItem economyItem = new MenuItem("经济");
        economyItem.setOnAction(e -> openDirectory("E:\\zzhongyang\\Knowledge\\经济"));
        menuKnowledge.getItems().addAll(knowledgeRootItem, economyItem);

        MenuItem helpItem = new MenuItem("打开目录索引文件");
        helpItem.setOnAction(e -> openFile("E:\\zzhongyang\\目录索引.txt"));
        menuHelp.getItems().add(helpItem);

        menuBar.getMenus().addAll(menuFile, menuEdit, menuWorkspace, menuTools, menuKnowledge, menuHelp);
        return menuBar;
    }

    private void openLogFileInEditor() {
        File logFile = new File("E:\\zzhongyang\\Kinlong Project\\ECS\\系统功能更新日志.txt");
        File parentDir = logFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();
        openTextFileForEditing(logFile, "系统功能更新日志");
    }

    private void resetToBlank() {
        root.setCenter(defaultCenterView);
        isInToolsManagement = false;
        editorPanel = null;
        editorTextArea = null;
        currentEditFile = null;
        statusLabel = null;
    }

    private void jumpToTarget(String input) {
        if (input.startsWith("http://") || input.startsWith("https://")) {
            openWebpage(input);
            return;
        }
        File f = new File(input);
        if (f.exists()) {
            openFileOrDir(input);
            return;
        }
        if (input.contains(".") && !input.contains(" ")) {
            String tryUrl = "https://" + input;
            if (isValidUrl(tryUrl)) {
                openWebpage(tryUrl);
                return;
            }
        }
        showAlert("无法识别输入内容，请输入有效的网址或存在的本地路径。\n示例：https://www.baidu.com 或 E:\\myfile.txt");
    }

    private boolean isValidUrl(String url) {
        try { new URI(url).toURL(); return true; } catch (Exception e) { return false; }
    }

    private void openFileOrDir(String path) {
        try { Desktop.getDesktop().open(new File(path)); } catch (IOException e) { showAlert("无法打开：" + e.getMessage()); }
    }

    private void openWebpage(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                showAlert("当前系统不支持打开浏览器，请手动访问：" + url);
            }
        } catch (Exception e) {
            showAlert("无法打开网页：" + e.getMessage());
        }
    }

    private void openDirectory(String path) {
        try {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) Desktop.getDesktop().open(dir);
            else showAlert("目录不存在或无效：" + path);
        } catch (IOException e) { showAlert("无法打开目录：" + e.getMessage()); }
    }

    private void openFile(String path) {
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) Desktop.getDesktop().open(file);
            else showAlert("文件不存在：" + path);
        } catch (IOException e) { showAlert("无法打开文件：" + e.getMessage()); }
    }

    private HBox createQuickAccessBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(5, 10, 5, 10));
        bar.setStyle("-fx-background-color: #e9ecef; -fx-border-color: #ced4da; -fx-border-width: 0 0 1 0;");
        bar.setAlignment(Pos.CENTER_LEFT);
        Label promptLabel = new Label("快速访问：");
        promptLabel.setStyle("-fx-font-weight: bold;");
        ComboBox<String> websiteCombo = new ComboBox<>();
        websiteCombo.getItems().addAll("淘宝网", "京东网", "我的钢铁网");
        websiteCombo.setPromptText("选择网站");
        websiteCombo.setPrefWidth(150);
        websiteCombo.setOnAction(e -> {
            String selected = websiteCombo.getValue();
            if (selected == null) return;
            String url = switch (selected) {
                case "淘宝网" -> "https://www.taobao.com";
                case "京东网" -> "https://www.jd.com";
                case "我的钢铁网" -> "https://www.mysteel.com";
                default -> null;
            };
            if (url != null) openWebpage(url);
            websiteCombo.getSelectionModel().clearSelection();
        });
        bar.getChildren().addAll(promptLabel, websiteCombo);
        return bar;
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