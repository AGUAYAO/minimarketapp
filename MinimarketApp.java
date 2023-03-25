package Clases;

import classes.Conexion;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class MinimarketApp {

    private JTextField barcodeField;
    private JTextField quantityField;
    private JTable productListTable;
    private JLabel totalLabel;

    private Connection connection;

    public MinimarketApp() {
        connection = Conexion.conectar();
        if (connection == null) {
            JOptionPane.showMessageDialog(null, "Error connecting to database.");
            System.exit(1);
        }

        JFrame frame = new JFrame("Minimarket App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(800, 600));

        JPanel contentPanel = new JPanel(new BorderLayout());

        JPanel inputPanel = createInputPanel();
        JPanel productListPanel = createProductListPanel();

        contentPanel.add(inputPanel, BorderLayout.NORTH);
        contentPanel.add(productListPanel, BorderLayout.CENTER);

        frame.setContentPane(contentPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        JLabel barcodeLabel = new JLabel("Barcode:");
        inputPanel.add(barcodeLabel, c);

        c.gridx = 1;
        barcodeField = new JTextField(20);
        inputPanel.add(barcodeField, c);

        c.gridx = 0;
        c.gridy = 1;
        JLabel quantityLabel = new JLabel("Quantity:");
        inputPanel.add(quantityLabel, c);

        c.gridx = 1;
        quantityField = new JTextField("1", 20);
        inputPanel.add(quantityField, c);

        barcodeField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String barcode = barcodeField.getText();
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT nombre, precio, stock FROM productos WHERE codigo = ?");
                    statement.setString(1, barcode);
                    ResultSet result = statement.executeQuery();
                    if (result.next()) {
                        String name = result.getString("nombre");
                        double price = result.getDouble("precio");
                        int stock = result.getInt("stock");
                        int quantity = Integer.parseInt(quantityField.getText());

                        if (quantity > stock) {
                            JOptionPane.showMessageDialog(null, "There is not enough stock for this product.");
                            return;
                        }

                        DefaultTableModel model = (DefaultTableModel) productListTable.getModel();
                        model.addRow(new Object[]{quantity, name, price, quantity * price});

                        updateTotal();

                        barcodeField.setText("");
                        quantityField.setText("1");
                        quantityField.requestFocus();
                    } else {
                        JOptionPane.showMessageDialog(null, "Product not found.");
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Error querying database: " + ex.getMessage());
                }
            }
        });

        quantityField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                barcodeField.requestFocus();
            }
        });

        return inputPanel;
    }

    private JPanel createProductListPanel() {
        JPanel productListPanel = new JPanel(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Quantity", "Description", "UVP", "Total"}, 0);
        productListTable = new JTable(model);
        productListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(productListTable);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        // Add mouse listener to remove selected item
        productListTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = productListTable.getSelectedRow();
                if (row != -1) {
                    int choice = JOptionPane.showConfirmDialog(null, "¿Seguro deseas eliminar el producto de la cuenta?");
                    if (choice == JOptionPane.YES_OPTION) {
                        DefaultTableModel model = (DefaultTableModel) productListTable.getModel();
                        String productName = (String) model.getValueAt(row, 1);
                        removeProductFromList(productName);
                        updateTotalLabel();
                    }
                }
            }
        });

        productListPanel.add(scrollPane, BorderLayout.CENTER);
        productListPanel.add(createTotalPanel(), BorderLayout.PAGE_END);

        return productListPanel;
    }

    /**
     * Removes a product from the list based on its name.
     *
     * @param productName the name of the product to be removed
     */
    private void removeProductFromList(String productName) {
        DefaultTableModel model = (DefaultTableModel) productListTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String name = (String) model.getValueAt(i, 1);
            if (name.equals(productName)) {
                model.removeRow(i);
                break;
            }
        }
        updateTotalLabel();
    }

    /**
     * Creates a panel with the total label.
     *
     * @return the panel with the total label
     */
    private JPanel createTotalPanel() {
        JPanel panel = new JPanel();
        totalLabel = new JLabel("Total: 0.0");
        panel.add(totalLabel);
        return panel;
    }

    /**
     * Updates the total label based on the values in the product list table.
     */
    private void updateTotalLabel() {
        double total = 0.0;
        DefaultTableModel model = (DefaultTableModel) productListTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            double price = Double.parseDouble(model.getValueAt(i, 2).toString());
            int quantity = Integer.parseInt(model.getValueAt(i, 0).toString());
            total += price * quantity;
        }
        totalLabel.setText(String.format("Total: %.2f", total));
    }

    /**
     * Decreases the stock of a product based on its name and the quantity sold.
     *
     * @param productName the name of the product to decrease stock of
     * @param quantitySold the quantity sold
     */
    private void decreaseProductStock(String productName, int quantitySold) {
        try {
            Connection cn = Conexion.conectar();
            PreparedStatement pst = cn.prepareStatement("UPDATE productos SET stock = stock - ? WHERE nombre = ?");
            pst.setInt(1, quantitySold);
            pst.setString(2, productName);
            pst.executeUpdate();
            cn.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar el stock del producto.");
        }
    }

    /**
     * Increases the stock of a product based on its name and the quantity
     * removed from the list.
     *
     * @param productName the name of the product to increase stock of
     * @param quantityRemoved the quantity removed from the list
     */
    private void increaseProductStock(String productName, int quantityRemoved) {
        try {
            Connection cn = Conexion.conectar();
            PreparedStatement pst = cn.prepareStatement("UPDATE productos SET stock = stock + ? WHERE nombre = ?");
            pst.setInt(1, quantityRemoved);
            pst.setString(2, productName);
            pst.executeUpdate();
            cn.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar la cantidad del producto en la base de datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

// Method to remove a product from the cart
    private void removeProductFromCart() {
        int selectedRow = productListTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Por favor selecciona un producto para eliminar de la lista.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(null, "¿Seguro deseas eliminar el producto de la cuenta?", "Confirmación", JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            DefaultTableModel model = (DefaultTableModel) productListTable.getModel();
            int quantity = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());
            String description = model.getValueAt(selectedRow, 1).toString();

            // Remove the product from the cart
            model.removeRow(selectedRow);

            // Update the total label
            double total = Double.parseDouble(totalLabel.getText().substring(1));
            double productTotal = Double.parseDouble(model.getValueAt(selectedRow, 3).toString().substring(1));
            total -= productTotal;
            totalLabel.setText("$" + String.format("%.2f", total));

            // Restore the stock of the product in the database
            try {
                Connection cn = Conexion.conectar();
                PreparedStatement pst = cn.prepareStatement("UPDATE productos SET cantidad = cantidad + ? WHERE descripcion = ?");
                pst.setInt(1, quantity);
                pst.setString(2, description);
                pst.executeUpdate();
                cn.close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error al actualizar la cantidad del producto en la base de datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

// Method to handle button clicks
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == barcodeField) {
            String barcode = barcodeField.getText().trim();
            Product product = getProductByBarcode(barcode);
            if (product != null) {
                descriptionField.setText(product.getDescription());
                quantityField.requestFocus();
            } else {
                JOptionPane.showMessageDialog(null, "Producto no encontrado.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (e.getSource() == addButton || e.getSource() == quantityField) {
            String description = descriptionField.getText().trim();
            int quantity = Integer.parseInt(quantityField.getText().trim());
            double unitPrice = getProductUnitPrice(description);
            if (unitPrice == -1) {
                JOptionPane.showMessageDialog(null, "Producto no encontrado.", "Error", JOptionPane.ERROR_MESSAGE);
            } else if (quantity <= 0) {
                JOptionPane.showMessageDialog(null, "Por favor ingresa una cantidad válida.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                double total = unitPrice * quantity;
                DefaultTableModel model = (DefaultTableModel) productListTable.getModel();
                model.addRow(new Object[]{quantity, description, "$" + String.format("%.2f", unitPrice), "$" + String.format("%.2f", total)});

                double currentTotal = Double.parseDouble(totalLabel.getText().substring(1));
                totalLabel.setText("$" + String.format("%.2f", currentTotal + total));

                updateProductStock(description, quantity);
            }

            descriptionField.setText("");
            barcodeField.setText("");
            quantityField.setText("1");
            barcodeField.requestFocus();
        } else if (e.getSource() == removeButton) {
            removeProductFromCart();
        } else if (e.getSource() == payButton) {
            int confirmation = JOptionPane.showConfirmDialog(null, "¿Estás seguro que deseas cobrar?", "Confirmación",
                    JOptionPane.YES_NO_OPTION);
            if (confirmation == JOptionPane.YES_OPTION) {
                double total = Double.parseDouble(totalLabel.getText());
                if (total > 0) {
                    try {
                        Connection cn = Conexion.conectar();
                        PreparedStatement ps = cn.prepareStatement("INSERT INTO ventas (fecha_venta, total_venta) VALUES (?,?)");
                        ps.setDate(1, new Date(Calendar.getInstance().getTimeInMillis()));
                        ps.setDouble(2, total);
                        ps.executeUpdate();
                        ResultSet rs = ps.getGeneratedKeys();
                        if (rs.next()) {
                            int id_venta = rs.getInt(1);
                            DefaultTableModel model = (DefaultTableModel) productListTable.getModel();
                            int num_rows = model.getRowCount();
                            for (int i = 0; i < num_rows; i++) {
                                int quantity = (int) model.getValueAt(i, 0);
                                String description = (String) model.getValueAt(i, 1);
                                double price = (double) model.getValueAt(i, 2);
                                double total_product = (double) model.getValueAt(i, 3);

                                PreparedStatement ps_detalle_venta = cn.prepareStatement("INSERT INTO detalle_venta (id_venta, descripcion, precio_unitario, cantidad, total_producto) VALUES (?,?,?,?,?)");
                                ps_detalle_venta.setInt(1, id_venta);
                                ps_detalle_venta.setString(2, description);
                                ps_detalle_venta.setDouble(3, price);
                                ps_detalle_venta.setInt(4, quantity);
                                ps_detalle_venta.setDouble(5, total_product);
                                ps_detalle_venta.executeUpdate();

                                PreparedStatement ps_actualizar_stock = cn.prepareStatement("UPDATE productos SET stock=stock-? WHERE codigo=?");
                                ps_actualizar_stock.setInt(1, quantity);
                                ps_actualizar_stock.setString(2, productList.get(i).getCodigo());
                                ps_actualizar_stock.executeUpdate();
                            }
                            productList.clear();
                            updateProductTable();
                            totalLabel.setText("0.00");
                            JOptionPane.showMessageDialog(null, "Venta realizada con éxito", "Información", JOptionPane.INFORMATION_MESSAGE);
                        }
                        cn.close();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(null, "Error al realizar la venta", "Error", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "No hay productos en la lista", "Información", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
        return panel;

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MinimarketApp app = new MinimarketApp();
                app.setVisible(true);
            }
        });
    }
}
