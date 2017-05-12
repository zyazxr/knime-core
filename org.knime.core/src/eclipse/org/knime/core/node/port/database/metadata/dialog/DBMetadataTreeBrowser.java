/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Feb 15, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.JDBCType;
import java.sql.SQLType;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.database.metadata.DBMetadataProvider;
import org.knime.core.node.port.database.metadata.filter.search.EmptyNodeFilter;
import org.knime.core.node.port.database.metadata.filter.search.SearchFilter;
import org.knime.core.node.port.database.metadata.filter.search.TableSearchFilter;
import org.knime.core.node.port.database.metadata.filter.selected.SelectedElementFilter;
import org.knime.core.node.port.database.metadata.filter.selection.SelectionFilter;
import org.knime.core.node.port.database.metadata.impl.FilteredDBMetadata;
import org.knime.core.node.port.database.metadata.model.DBColumn;
import org.knime.core.node.port.database.metadata.model.DBColumnContainer;
import org.knime.core.node.port.database.metadata.model.DBMetadata;
import org.knime.core.node.port.database.metadata.model.DBObject;
import org.knime.core.node.port.database.metadata.model.DBSchema;
import org.knime.core.node.port.database.metadata.model.DBTable;
import org.knime.core.node.port.database.metadata.model.DBView;
import org.knime.core.util.SwingWorkerWithContext;

/**
 * The metadata tree browser. This panel contains the metadata tree browser and a refresh button to refresh the content
 * of the tree.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @since 3.4
 */
public class DBMetadataTreeBrowser extends JPanel {

    /**
     * The property name for double clicking an object in the browser. Needed for the PropertyListener.
     */
    public final static String DOUBLE_CLICK_PROP_NAME = "doubleClickOk";

    private static final long serialVersionUID = 1;

    private static final ImageIcon TABLE_ICON =
        new ImageIcon(DBMetadataTreeBrowser.class.getResource("icons/table.png"));

    private static final ImageIcon VIEW_ICON = new ImageIcon(DBMetadataTreeBrowser.class.getResource("icons/view.png"));

    private static final ImageIcon SCHEMA_ICON =
        new ImageIcon(DBMetadataTreeBrowser.class.getResource("icons/schema.png"));

    private static final ImageIcon TABLE_CATEGORY_ICON =
        new ImageIcon(DBMetadataTreeBrowser.class.getResource("icons/table_category.png"));

    private static final ImageIcon VIEW_CATEGORY_ICON =
        new ImageIcon(DBMetadataTreeBrowser.class.getResource("icons/view_category.png"));

    private static final Map<SQLType, DataType> DATA_TYPE_MAPPING = getTypeMapping();

    private static final DBObject SCHEMA_LABEL = createDBObject("SCHEMA");

    private static final DBObject TABLE_LABEL = createDBObject("TABLE");

    private static final DBObject VIEW_LABEL = createDBObject("VIEW");

    private static final DBObject UNKNOWN_LABEL = createDBObject("UNKNOWN");

    private final JTree m_tree;

    private final DefaultMutableTreeNode m_root = new DefaultMutableTreeNode(createDBObject("ROOT"));

    private final DBMetadataProvider m_dbmetaProvider;

    private final JTextArea m_infoMessage = initializeInfoMessage();

    private final JScrollPane m_scrollPane = new JScrollPane();

    private final JButton m_refresh = new JButton("Refresh");

    private final SelectionFilter m_selectionFilter;

    private SwingWorker<Void, Void> m_worker;

    private SelectedElementFilter m_selElementFilter;

    /**
     * Create a DB object for the labels in the tree, e.g the nodes called "SCHEMA", "TABLE", etc.
     *
     * @return the DB object containing the label
     */
    private static DBObject createDBObject(final String name) {
        return new DBObject() {

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String toString() {
                return getName();
            }
        };
    }

    /**
     * Constructor for the metadata tree browser.
     *
     * @param metadataProvider the metadata provider
     * @param selectionFilter the selection filter
     * @param selElementFilter the selected element filter
     */
    public DBMetadataTreeBrowser(final DBMetadataProvider metadataProvider, final SelectionFilter selectionFilter,
        final SelectedElementFilter selElementFilter) {
        super(new BorderLayout());
        m_selectionFilter = selectionFilter;
        m_selElementFilter = selElementFilter;
        m_dbmetaProvider = metadataProvider;
        m_tree = new JTree(m_root);
        m_tree.setRootVisible(false);
        m_tree.setToggleClickCount(1);
        m_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        m_tree.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public final void mouseClicked(final MouseEvent me) {
                if (me.getClickCount() == 2) {
                    firePropertyChange(DOUBLE_CLICK_PROP_NAME, false, true);
                    lazyLoading();
                }
            }
        });

        m_tree.setCellRenderer(new DefaultTreeCellRenderer() {
            private static final long serialVersionUID = 2873103713192152036L;

            @Override
            public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
                final boolean expanded, final boolean leaf, final int row, final boolean hasfocus) {

                String text = value.toString();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
                Icon usedIcon = getNodeIcon(node.getUserObject());
                StringBuffer html = new StringBuffer("<html>");
                if (node.getUserObject() instanceof DBObject) {
                    DBObject nodeObject = (DBObject)node.getUserObject();
                    if (m_selElementFilter.filter(nodeObject)) {
                        html.append("<b>" + text + "</b>");
                    } else {
                        html.append(text);
                    }
                } else {
                    html.append(text);
                }
                html.append("</html>");

                Component result =
                    super.getTreeCellRendererComponent(tree, html.toString(), sel, expanded, leaf, row, hasfocus);

                if (usedIcon != null) {
                    setIcon(usedIcon);
                }
                return result;
            }
        });

        if (m_dbmetaProvider.getMetadata() == null || !closeWorker()) {
            m_infoMessage.setBackground(m_tree.getBackground());
            m_scrollPane.setViewportView(m_infoMessage);
            fetchMetadata();
        } else {
            m_scrollPane.setViewportView(m_tree);
        }

        m_refresh.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                fetchMetadata();
            }

        });

        m_scrollPane.setPreferredSize(new Dimension(100, 400));
        super.add(m_scrollPane, BorderLayout.CENTER);
        super.add(m_refresh, BorderLayout.SOUTH);

        super.setVisible(true);
    }

    /**
     * Update this tree and metadata from database.
     *
     * @param filter the DB metadata where the elements will be fetched
     */
    public final void updateTreeFilter(final SearchFilter filter) {
        DBMetadata dbmetadata = m_dbmetaProvider.getMetadata();

        if (filter != null) {
            dbmetadata = new FilteredDBMetadata(dbmetadata, filter);
            if (filter instanceof TableSearchFilter) {
                dbmetadata = new FilteredDBMetadata(dbmetadata, new EmptyNodeFilter(true));
            } else {
                dbmetadata = new FilteredDBMetadata(dbmetadata, new EmptyNodeFilter(false));
            }
        }
        m_tree.collapsePath(new TreePath(m_root));
        m_root.removeAllChildren();
        ((DefaultTreeModel)m_tree.getModel()).reload(m_root);

        if (dbmetadata != null) {
            Iterable<DBSchema> schemas = dbmetadata.getSchemas();
            final DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode(SCHEMA_LABEL);
            schemaNode.setAllowsChildren(true);

            for (final DBSchema schema : schemas) {
                DefaultMutableTreeNode schemaName;
                if (schema.getName().isEmpty()) {
                    schemaName = new DefaultMutableTreeNode(UNKNOWN_LABEL);
                } else {
                    schemaName = new DefaultMutableTreeNode(schema);
                }

                schemaName.setAllowsChildren(true);

                DefaultMutableTreeNode tableNode = createChildrenNodes(TABLE_LABEL, schema.getTables());

                if (tableNode.getChildCount() > 0) {
                    schemaName.add(tableNode);
                }

                DefaultMutableTreeNode viewNode = createChildrenNodes(VIEW_LABEL, schema.getViews());

                if (viewNode.getChildCount() > 0) {
                    schemaName.add(viewNode);
                }

                schemaNode.add(schemaName);
            }

            m_root.add(schemaNode);

            if (filter != null) {
                expandAllNodes();
            } else {
                setTreeViewToDefault();
            }

            m_tree.validate();
            m_tree.repaint();

            m_scrollPane.setViewportView(m_tree);
        }
    }

    /**
     * Create a node for a DBObject that will also contain its children as nodes.
     *
     * @param parentNodeName the parent DBObject
     * @param elements the children of the DBObject. The type should be DBColumnContainer
     * @return the parent node containing its children as sub-nodes as well
     */
    private DefaultMutableTreeNode createChildrenNodes(final DBObject parentNodeName,
        final Iterable<? extends DBColumnContainer> elements) {
        DefaultMutableTreeNode parentNode = new DefaultMutableTreeNode(parentNodeName);
        parentNode.setAllowsChildren(true);

        for (final DBColumnContainer obj : elements) {
            DefaultMutableTreeNode objName = new DefaultMutableTreeNode(obj);
            addColumnNodes(objName, obj.getColumns());
            parentNode.add(objName);
        }
        return parentNode;
    }

    /**
     * Create columns nodes and add them to the corresponding table node.
     *
     * @param node the table node
     * @param columns the columns that should be converted to nodes
     */
    private void addColumnNodes(final DefaultMutableTreeNode node, final Iterable<DBColumn> columns) {
        if (node == null) {
            return;
        }
        if (node.getUserObject() instanceof DBColumnContainer && columns != null) {
            node.setAllowsChildren(true);
            for (DBColumn column : columns) {
                final DefaultMutableTreeNode child = new DefaultMutableTreeNode(column);
                child.setAllowsChildren(false);
                node.add(child);
            }
        }

    }

    /**
     * Fetch the metadata from the database. This process could take a while and will be done in background.
     */
    private void fetchMetadata() {
        m_worker = new SwingWorkerWithContext<Void, Void>() {

            /** {@inheritDoc} */
            @Override
            protected Void doInBackgroundWithContext() {
                try {
                    m_refresh.setEnabled(false);
                    m_infoMessage
                        .setText("Fetching metadata...\n\nPlease note that this might take some time depending "
                            + "on the size of the database.\n\nDouble-clicking on a table/view would load its columns.");
                    m_scrollPane.setViewportView(m_infoMessage);
                    m_dbmetaProvider.refresh();
                    updateTreeFilter(null);
                } catch (Exception ex) {
                    m_infoMessage.setText("Error during fetching metadata from database, reason: " + ex.getMessage());
                    m_scrollPane.setViewportView(m_infoMessage);
                }
                return null;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void doneWithContext() {
                m_refresh.setEnabled(true);
            }
        };
        m_worker.execute();
    }

    /**
     * Set the tree to the default view (expanded to the selected object, if exists).
     */
    private void setTreeViewToDefault() {
        m_tree.expandPath(new TreePath(m_root));
        expandSchemas();
        updateSelectedObject(m_selElementFilter);
    }

    /**
     * Expand all nodes down to the table nodes.
     */
    private void expandAllNodes() {
        DefaultMutableTreeNode currentNode = m_root.getNextNode();

        while (currentNode != null) {
            if (!currentNode.isLeaf() && currentNode.getChildCount() != 0
                && !(currentNode.getUserObject() instanceof DBColumnContainer)) {
                m_tree.expandPath(new TreePath(currentNode.getPath()));
            }
            currentNode = currentNode.getNextNode();
        }

    }

    /**
     * Check if there are more than 1 schema. If only 1 schema exists, expand the schema. Else, do nothing. This should
     * be done recursively to the children as well.
     */
    private void expandSchemas() {
        DefaultMutableTreeNode currentNode = m_root.getNextNode();

        m_tree.expandPath(new TreePath(currentNode.getPath()));
        if (currentNode.getChildCount() == 1) {
            expandChildrenNodes(currentNode);
        }
    }

    /**
     * Expand nodes that don't have any siblings.
     *
     * @param parentNode the parent node that doesn't have siblings.
     */
    private void expandChildrenNodes(final DefaultMutableTreeNode parentNode) {
        int currentLevel = parentNode.getLevel() + 1;
        DefaultMutableTreeNode currentNode = parentNode.getNextNode();
        while (currentNode != null) {
            if (currentNode.getLevel() == currentLevel) {
                m_tree.expandPath(new TreePath(currentNode.getPath()));
                if (currentNode.getChildCount() == 1) {
                    expandChildrenNodes(currentNode);
                }
            }
            currentNode = currentNode.getNextNode();

        }

    }

    /**
     * Search the selected DBObject. This method is needed so that after building the tree, the path to the selected
     * object (if not null) can be immediately expanded.
     *
     * @return the selected node
     */
    @SuppressWarnings("unchecked")
    private DefaultMutableTreeNode searchSelectedNode() {
        DefaultMutableTreeNode selectedNode = null;
        for (Enumeration<DefaultMutableTreeNode> e = m_root.depthFirstEnumeration(); e.hasMoreElements()
            && selectedNode == null;) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node.getUserObject() instanceof DBObject) {
                if (m_selElementFilter.filter((DBObject)node.getUserObject())) {
                    selectedNode = node;
                }
            }

        }
        return selectedNode;
    }

    /**
     * Close the swing worker or cancel if it's done.
     *
     * @return false if the worker is cancelled before the job is done, true otherwise.
     */
    public boolean closeWorker() {
        if (m_worker == null) {
            return false;
        }
        if (m_worker != null && !m_worker.isDone()) {
            m_worker.cancel(true);
            return false;
        }
        return true;
    }

    /**
     * Get the selected object in the tree.
     *
     * @return the selected object
     */
    public DBObject getSelectedObject() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_tree.getLastSelectedPathComponent();
        if (node == null) {
            return null;
        }
        DBObject selectedObject = (DBObject)node.getUserObject();
        if (m_selectionFilter.filter(selectedObject)) {
            return selectedObject;
        }
        return null;
    }

    /**
     * Update the selected object. This should be called every time the tree browser dialog window is opened.
     *
     * @param selElementFilter the updated selected element filter that contains the new values
     */
    public void updateSelectedObject(final SelectedElementFilter selElementFilter) {
        m_selElementFilter = selElementFilter;
        DefaultMutableTreeNode selectedNode = searchSelectedNode();
        if (selectedNode != null) {
            TreePath selectedNodePath = new TreePath(selectedNode.getPath());
            m_tree.expandPath(selectedNodePath);
            m_tree.setSelectionPath(selectedNodePath);
        } else {
            m_tree.clearSelection();
        }
    }

    /**
     * Get the corresponding node icon.
     *
     * @param obj the DBObject, whose icon should be returned
     * @return the icon for the DBObject
     */
    private Icon getNodeIcon(final Object obj) {
        Icon nodeIcon = null;

        if (obj instanceof DBObject) {
            DBObject nodeObject = (DBObject)obj;
            if (nodeObject instanceof DBSchema) {
                nodeIcon = SCHEMA_ICON;
            } else if (nodeObject instanceof DBTable) {
                nodeIcon = TABLE_ICON;
            } else if (nodeObject instanceof DBView) {
                nodeIcon = VIEW_ICON;
            } else if (nodeObject instanceof DBColumn) {
                DataType dataType = getKnimeDataType(((DBColumn)nodeObject).getColumnType());
                if (dataType != null) {
                    nodeIcon = dataType.getIcon();
                }
            }
        } else {
            if (obj.equals(TABLE_LABEL)) {
                nodeIcon = TABLE_CATEGORY_ICON;
            } else if (obj.equals(VIEW_LABEL)) {
                nodeIcon = VIEW_CATEGORY_ICON;
            }
        }
        return nodeIcon;
    }

    /**
     * Initialize the info message area, where any error message will be shown.
     *
     * @return the JTextArea
     */
    private JTextArea initializeInfoMessage() {
        JTextArea area = new JTextArea();
        area.setOpaque(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setLineWrap(true);
        return area;
    }

    /**
     * Do lazy loading for the table columns. This method will fetch the currently selected node in the tree and if the
     * node is a table, load the columns.
     */
    private void lazyLoading() {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_tree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        // Lazy loading of table columns
        if (node.getChildCount() == 0 && node.getUserObject() instanceof DBColumnContainer) {
            DBColumnContainer table = (DBColumnContainer)node.getUserObject();
            Iterable<DBColumn> columns = table.getNewColumnsIfEmpty();
            if (columns != null) {
                addColumnNodes(node, columns);
                m_tree.expandPath(new TreePath(node.getPath()));
            } else {
                DefaultMutableTreeNode errorNode = new DefaultMutableTreeNode("Error fetching columns.", false);
                node.add(errorNode);
            }
        }
    }

    private static DataType getKnimeDataType(final SQLType javaSqlType) {
        return DATA_TYPE_MAPPING.get(javaSqlType);
    }

    private static Map<SQLType, DataType> getTypeMapping() {
        Map<SQLType, DataType> map = new HashMap<SQLType, DataType>();
        DataType generalDataType = DataType.getType(DataCell.class);
        map.put(JDBCType.ARRAY, ListCell.getCollectionType(generalDataType));
        map.put(JDBCType.BIGINT, LongCell.TYPE);
        map.put(JDBCType.BINARY, BinaryObjectDataCell.TYPE);
        map.put(JDBCType.BIT, BooleanCell.TYPE);
        map.put(JDBCType.BLOB, BinaryObjectDataCell.TYPE);
        map.put(JDBCType.BOOLEAN, BooleanCell.TYPE);
        map.put(JDBCType.CHAR, StringCell.TYPE);
        map.put(JDBCType.CLOB, generalDataType);
        map.put(JDBCType.DATALINK, generalDataType);
        map.put(JDBCType.DATE, DateAndTimeCell.TYPE);
        map.put(JDBCType.DECIMAL, DoubleCell.TYPE);
        map.put(JDBCType.DISTINCT, generalDataType);
        map.put(JDBCType.DOUBLE, DoubleCell.TYPE);
        map.put(JDBCType.FLOAT, DoubleCell.TYPE);
        map.put(JDBCType.INTEGER, IntCell.TYPE);
        map.put(JDBCType.JAVA_OBJECT, generalDataType);
        map.put(JDBCType.LONGNVARCHAR, StringCell.TYPE);
        map.put(JDBCType.LONGVARBINARY, BinaryObjectDataCell.TYPE);
        map.put(JDBCType.LONGVARCHAR, StringCell.TYPE);
        map.put(JDBCType.NCHAR, StringCell.TYPE);
        map.put(JDBCType.NCLOB, generalDataType);
        map.put(JDBCType.NULL, generalDataType);
        map.put(JDBCType.NUMERIC, DoubleCell.TYPE);
        map.put(JDBCType.NVARCHAR, StringCell.TYPE);
        map.put(JDBCType.OTHER, generalDataType);
        map.put(JDBCType.REAL, DoubleCell.TYPE);
        map.put(JDBCType.REF, generalDataType);
        map.put(JDBCType.REF_CURSOR, generalDataType);
        map.put(JDBCType.ROWID, generalDataType);
        map.put(JDBCType.SMALLINT, IntCell.TYPE);
        map.put(JDBCType.SQLXML, generalDataType);
        map.put(JDBCType.STRUCT, generalDataType);
        map.put(JDBCType.TIME, DateAndTimeCell.TYPE);
        map.put(JDBCType.TIME_WITH_TIMEZONE, DateAndTimeCell.TYPE);
        map.put(JDBCType.TIMESTAMP, DateAndTimeCell.TYPE);
        map.put(JDBCType.TIMESTAMP_WITH_TIMEZONE, DateAndTimeCell.TYPE);
        map.put(JDBCType.TINYINT, IntCell.TYPE);
        map.put(JDBCType.VARBINARY, BinaryObjectDataCell.TYPE);
        map.put(JDBCType.VARCHAR, StringCell.TYPE);
        return map;
    }
}
