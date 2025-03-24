package ir.ac.kntu.twolevelhashtable.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.ac.kntu.twolevelhashtable.table.TwoLevelHashTable;
import redis.clients.jedis.Jedis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class HashTableGui extends JFrame {
    private TwoLevelHashTable<Integer, String> hashTable;
    private JTextField keyField, valueField;
    private JLabel collisionRateLabel;
    private JComboBox<String> hashFunctionSelector;
    private Jedis jedis;
    private ObjectMapper objectMapper;
    private JLabel redisIconLabel;

    public HashTableGui() {
        setTitle("جدول هش دو طبقه               |               ساختمان‌داده");
        setSize(1024, 666);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        this.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        UIManager.put("Label.isHtml", Boolean.TRUE);

        objectMapper = new ObjectMapper();
        jedis = new Jedis("localhost", 6379);
        hashTable = new TwoLevelHashTable<>(key -> key % 100, 100);

        try {
            Font vazirmatnFont = new Font("Vazirmatn", Font.BOLD, 16);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(vazirmatnFont);

            UIManager.put("Label.font", vazirmatnFont);
            UIManager.put("Button.font", vazirmatnFont);
            UIManager.put("TextField.font", vazirmatnFont);
            UIManager.put("ComboBox.font", vazirmatnFont);

        } catch (Exception e) {
            e.printStackTrace();
        }

        JPanel controlPanel = new JPanel(new GridLayout(2, 2, 2, 2));
        controlPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        redisIconLabel = new JLabel();
        ImageIcon redisIcon = new ImageIcon(getClass().getResource("/redis-icon.png"));
        redisIconLabel.setIcon(redisIcon);
        redisIconLabel.setPreferredSize(new Dimension(50, 50));

        ImageIcon originalIcon = new ImageIcon("redis-icon.png");
        Image scaledImage = originalIcon.getImage().getScaledInstance(200, 120, Image.SCALE_SMOOTH);
        ImageIcon resizedIcon = new ImageIcon(scaledImage);

        JLabel redisText = new JLabel("+ قابلیت ذخیره‌سازی جدول هش در ردیس");
        redisText.setFont(new Font("Vazirmatn", Font.BOLD, 14));
        redisText.setHorizontalAlignment(SwingConstants.CENTER);
        redisText.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel logoLabel = new JLabel(resizedIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.add(logoLabel);
        logoPanel.add(redisText);

        logoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(logoPanel, BorderLayout.NORTH);
        mainPanel.add(controlPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.NORTH);

        hashFunctionSelector = new JComboBox<>(new String[]{"هش hashCode", "هش باقی‌مانده", "هش ضرب", "هش اول"});
        hashFunctionSelector.addActionListener(this::onHashFunctionChange);
        controlPanel.add(hashFunctionSelector);
        controlPanel.add(new JLabel("تابع هش:"));


        valueField = new JTextField();
        controlPanel.add(valueField);
        controlPanel.add(new JLabel("مقدار:"));

        keyField = new JTextField();
        controlPanel.add(keyField);
        controlPanel.add(new JLabel("کلید:"));

        addButtons(controlPanel);
        add(createDisplayPanel(), BorderLayout.CENTER);
        add(createCollisionPanel(), BorderLayout.SOUTH);
        updateDisplay();
    }


    private void onHashFunctionChange(ActionEvent e) {
        String selectedHashFunction = (String) hashFunctionSelector.getSelectedItem();
        Function<Integer, Integer> newHashFunction;

        switch (selectedHashFunction) {
            case "هش باقی‌مانده":
                newHashFunction = key -> key % 10;
                break;
            case "هش ضرب":
                newHashFunction = key -> (int)((key * 0.6180339887) % 1 * 10);
                break;
            case "هش اول":
                newHashFunction = key -> key % 17;
                break;
            default:
                newHashFunction = Object::hashCode;
                break;
        }

        hashTable.setHashCipher(newHashFunction);
        refreshTable();
        updateDisplay();
        repaint();
        JOptionPane.showMessageDialog(this, "تابع هش بروزرسانی شده و جدول هش نیز بروزرسانی شد.");
    }

    private void refreshTable() {
        Map<Integer, String> primaryTableCopy = new HashMap<>(hashTable.getPrimaryTable());
        Map<Integer, LinkedList<String>> secondaryTableCopy = new HashMap<>(hashTable.getSecondaryTable());

        hashTable.clear();

        for (Map.Entry<Integer, String> entry : primaryTableCopy.entrySet()) {
            hashTable.insert(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<Integer, LinkedList<String>> entry : secondaryTableCopy.entrySet()) {
            for (String value : entry.getValue()) {
                hashTable.insert(entry.getKey(), value);
            }
        }

        updateDisplay();
        repaint();
    }


    private void addButtons(JPanel panel) {
        String[] buttonLabels = {"افزودن کلید جدید", "حذف کلید", "جست‌جوی کلید", "حذف کل جدول", "ذخیره در ردیس", "بازیابی از ردیس", "ذخیره در فایل json", "بازیابی از فایل json"};
        ActionListener[] actions = {this::insertAction, this::deleteAction, this::searchAction, e -> clearAction(), this::saveToRedis, this::loadFromRedis, this::saveToJson, this::loadFromJson};

        for (int i = 0; i < buttonLabels.length; i++) {
            JButton button = new JButton(buttonLabels[i]);
            if(buttonLabels[i].equals("حذف کلید")){
                button.setBackground(Color.RED);
                button.setForeground(Color.RED);
            } else if(buttonLabels[i].equals("افزودن کلید جدید")){
                button.setBackground(Color.BLUE);
                button.setForeground(Color.BLUE);
            } else if(buttonLabels[i].equals("جست‌جوی کلید")){
                button.setBackground(Color.green);
                button.setForeground(Color.green);
            } else if(buttonLabels[i].equals("حذف کل جدول")){
                button.setBackground(Color.gray);
                button.setForeground(Color.gray);
            } else if(buttonLabels[i].equals("ذخیره در ردیس")){
                button.setBackground(Color.orange);
                button.setForeground(Color.orange);
            } else if(buttonLabels[i].equals("بازیابی از ردیس")){
                button.setBackground(Color.magenta);
                button.setForeground(Color.magenta);
            } else if(buttonLabels[i].equals("ذخیره در فایل json")){
                button.setBackground(Color.PINK);
                button.setForeground(Color.PINK);
            } else if(buttonLabels[i].equals("بازیابی از فایل json")){
                button.setBackground(Color.GREEN);
                button.setForeground(Color.GREEN);
            } else {
                button.setBackground(Color.BLACK);
                button.setForeground(Color.BLACK);
            }

            button.addActionListener(actions[i]);
            panel.add(button);
        }
    }

    private void deleteAction(ActionEvent e) {
        try {
            int key = Integer.parseInt(keyField.getText());

            if (hashTable.delete(key)) {
                JOptionPane.showMessageDialog(this, "کلید " + key + " با موفقیت حذف شد. ");
            } else {
                JOptionPane.showMessageDialog(this, "کلید یافت نشد.");
            }

            updateDisplay();
            repaint();
            keyField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "کلید وارد شده نامعتبر است و باید مقداری عددی باشد.");
        }
        refreshTable();
    }

    private void searchAction(ActionEvent e) {
        try {
            int key = Integer.parseInt(keyField.getText());
            String result = String.valueOf(hashTable.search(key));

            if (result != null) {
                JOptionPane.showMessageDialog(this, "مقدار " + result + " یافت شد. ");
            } else {
                JOptionPane.showMessageDialog(this, "کلید مورد نظر یافت نشد.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "کلید وارد شده نامعتبر است و باید مقداری عددی باشد.");
        }
    }

    private void saveToJson(ActionEvent e) {
        try {
            Map<Integer, String> primaryTable = hashTable.getPrimaryTable();
            Map<Integer, LinkedList<String>> secondaryTable = hashTable.getSecondaryTable();

            File file = new File("hashtable_data.json");
            objectMapper.writeValue(file, Map.of("primaryTable", primaryTable, "secondaryTable", secondaryTable));

            JOptionPane.showMessageDialog(this, "جدول هش با موفقیت در فایل json ذخیره شد.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "خطا در ذخیره فایل json");
        }
    }

    private void loadFromJson(ActionEvent e) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(this);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                Map<String, Object> data = objectMapper.readValue(file, Map.class);

                Map<String, String> primaryTableData = objectMapper.convertValue(data.get("primaryTable"), Map.class);
                Map<String, List<String>> secondaryTableData = objectMapper.convertValue(data.get("secondaryTable"), Map.class);

                Map<Integer, String> primaryTable = new HashMap<>();
                for (Map.Entry<String, String> entry : primaryTableData.entrySet()) {
                    primaryTable.put(Integer.parseInt(entry.getKey()), entry.getValue());
                }

                Map<Integer, LinkedList<String>> secondaryTable = new HashMap<>();
                for (Map.Entry<String, List<String>> entry : secondaryTableData.entrySet()) {
                    secondaryTable.put(Integer.parseInt(entry.getKey()), new LinkedList<>(entry.getValue()));
                }

                hashTable.clear();
                primaryTable.forEach(hashTable::insert);
                secondaryTable.forEach((key, values) -> values.forEach(value -> hashTable.insert(key, value)));

                JOptionPane.showMessageDialog(this, "بازیابی جدول هش از فایل JSON با موفقیت انجام شد.");
                updateDisplay();
                repaint();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "خطا در بازیابی جدول هش از فایل JSON");
        }
    }



    private JPanel createDisplayPanel() {
        JPanel displayPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        displayPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        displayPanel.add(new TablePanel(hashTable, true));
        displayPanel.add(new TablePanel(hashTable, false));
        return displayPanel;
    }

    private JPanel createCollisionPanel() {
        JPanel collisionPanel = new JPanel();
        collisionPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        collisionRateLabel = new JLabel("نرخ برخورد: 0.0");
        collisionPanel.add(collisionRateLabel);
        return collisionPanel;
    }

    private void insertAction(ActionEvent e) {
        try {
            int key = Integer.parseInt(keyField.getText());
            String value = valueField.getText();
            hashTable.insert(key, value);
            updateDisplay();
            keyField.setText("");
            valueField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "کلید وارد شده نامعتبر است و باید مقداری عددی باشد.");
        }
        refreshTable();
    }

    private void saveToRedis(ActionEvent e) {
        try {
            jedis.del("primaryTable");
            jedis.del("secondaryTable");

            String primaryTableJson = objectMapper.writeValueAsString(hashTable.getPrimaryTable());
            String secondaryTableJson = objectMapper.writeValueAsString(hashTable.getSecondaryTable());

            jedis.set("primaryTable", primaryTableJson);
            jedis.set("secondaryTable", secondaryTableJson);

            JOptionPane.showMessageDialog(this, "جدول هش با موفقیت در دیتابیس ردیسی ذخیره شد.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "خطا در ذخیره‌سازی جدول هش در ردیس");
        }
    }

    private void loadFromRedis(ActionEvent e) {
        try {
            String primaryTableJson = jedis.get("primaryTable");
            String secondaryTableJson = jedis.get("secondaryTable");

            if (primaryTableJson != null && secondaryTableJson != null) {
                Map<String, Object> primaryTableData = objectMapper.readValue(primaryTableJson, Map.class);
                Map<String, Object> secondaryTableData = objectMapper.readValue(secondaryTableJson, Map.class);

                Map<Integer, String> primaryTable = new HashMap<>();
                for (Map.Entry<String, Object> entry : primaryTableData.entrySet()) {
                    Integer key = Integer.parseInt(entry.getKey());
                    String value = (String) entry.getValue();
                    primaryTable.put(key, value);
                }

                Map<Integer, List<String>> secondaryTable = new HashMap<>();
                for (Map.Entry<String, Object> entry : secondaryTableData.entrySet()) {
                    Integer key = Integer.parseInt(entry.getKey());
                    List<String> values = (List<String>) entry.getValue();
                    secondaryTable.put(key, values);
                }

                hashTable.clear();

                primaryTable.forEach((key, value) -> hashTable.insert(key, value));
                secondaryTable.forEach((key, values) -> values.forEach(value -> hashTable.insert(key, value)));

                JOptionPane.showMessageDialog(this, "جدول هش با موفقیت از دیتابیس ردیسی بازیابی شد.");
                updateDisplay();
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, "هیچ جدول هشی در دیتابیس ردیسی یافت نشد.");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "خطا در بازیابی جدول هش از دیتابیس ردیسی");
        }
    }

    private void clearAction() {
        hashTable.clear();
        updateDisplay();
        repaint();
        refreshTable();
    }

    private void updateDisplay() {
        collisionRateLabel.setText("نرخ برخورد: " + hashTable.getCollisionRate());
    }
}

class TablePanel extends JPanel {
    private TwoLevelHashTable<Integer, String> hashTable;
    private boolean isPrimaryTable;

    public TablePanel(TwoLevelHashTable<Integer, String> hashTable, boolean isPrimaryTable) {
        this.hashTable = hashTable;
        this.isPrimaryTable = isPrimaryTable;
        setPreferredSize(new Dimension(480, 600));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        int x = 20, y = 30, rowHeight = 30;

        g.drawString("Bucket ID", x, y);
        g.drawString("(Key, Value)", x + 100, y);
        y += rowHeight;

        if (isPrimaryTable) {
            for (Map.Entry<Integer, String> entry : hashTable.getPrimaryTable().entrySet()) {
                int bucketId = hashTable.getHashCipher().apply(entry.getKey()) % hashTable.getCapacity();

                g.drawString(String.valueOf(bucketId), x, y);
                g.drawString("(" + entry.getKey() + ", " + entry.getValue() + ")", x + 100, y);
                y += rowHeight;
            }
        } else {
            for (Map.Entry<Integer, LinkedList<String>> entry : hashTable.getSecondaryTable().entrySet()) {
                int bucketId = hashTable.getHashCipher().apply(entry.getKey()) % hashTable.getCapacity();

                boolean firstEntry = true;
                for (String value : entry.getValue()) {
                    if (firstEntry) {
                        g.drawString(String.valueOf(bucketId), x, y);
                        firstEntry = false;
                    }
                    g.drawString("(" + entry.getKey() + ", " + value + ")", x + 100, y);
                    y += rowHeight;
                }
            }
        }
    }
}






