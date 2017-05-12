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
 *   Feb 21, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.knime.core.node.port.database.metadata.model.DBObject;

import schemacrawler.schema.NamedObject;

/**
 * An Iterator for the database objects. The purpose of this iterator is to iterate through a list of database objects
 * from SchemaCrawler and at the same time wrap them in DBObject so that they are recognizable within KNIME.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @param <T> DBObject
 * @param <S> SchemaCrawler object
 * @since 3.4
 */
public class SchemaCrawlerCollection<T extends DBObject, S extends NamedObject> implements Iterable<T> {

    private final NamedObject m_source;

    private final Collection<T> m_elements;

    private final ElementCreator<T, S> m_creator;

    /**
     * Constructor for the SchemaCrawlerCollection.
     *
     * @param source the source object from SchemaCrawler. Example: if the source is a Catalog, then we can iterate
     *            though the schemas that are contained in the catalog
     * @param creator the ElementCreator to wrap the SchemaCrawler object into KNIME DBobject
     */
    public SchemaCrawlerCollection(final NamedObject source, final ElementCreator<T, S> creator) {
        m_source = source;
        m_creator = creator;
        m_elements = createElements(m_creator.getElements(m_source));
    }

    /**
     * Wrap the input collection to a new collection.
     *
     * @param elements the input collection
     * @return the new wrapped collection
     */
    private Collection<T> createElements(final Collection<S> elements) {
        final Collection<T> wrappedElements = new ArrayList<>(elements.size());
        for (S s : elements) {
            wrappedElements.add(m_creator.wrapElement(s));
        }
        return wrappedElements;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator() {
        return m_elements.iterator();
    }
}
