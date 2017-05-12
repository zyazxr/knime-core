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
 *   Apr 20, 2017 (adewi): created
 */
package org.knime.core.node.port.database.metadata.impl;

import java.util.Iterator;

import org.knime.core.node.port.database.metadata.filter.search.SearchFilter;
import org.knime.core.node.port.database.metadata.model.DBObject;

/**
 * An iterator for filtering DBObjects. It will iterate an array of DBObjects and apply a filter to each one.
 *
 * @author Andisa Dewi, KNIME.com, Berlin, Germany
 * @param <D> DBObject
 * @since 3.4
 */
public class DBObjectFilterIterator<D extends DBObject> implements Iterator<D>, Iterable<D> {

    private final Iterator<D> m_iter;

    private final SearchFilter m_filter;

    private final ElementWrapper<D, D> m_creator;

    private D m_next;

    /**
     * @param iter the input Iterable
     * @param filter the filter to be applied to each DBObject
     * @param elementCreator the wrapper to wrap each DBObject
     *
     */
    public DBObjectFilterIterator(final Iterable<D> iter, final SearchFilter filter,
        final ElementWrapper<D, D> elementCreator) {
        m_iter = iter.iterator();
        m_filter = filter;
        m_creator = elementCreator;
        m_next = getNext();
    }

    /**
     * Get the next element. Only elements that pass the filter will be returned.
     *
     * @return the next element that has been wrapped
     */
    private D getNext() {
        while (m_iter.hasNext()) {
            D nextElem = m_iter.next();
            if (m_filter.filter(nextElem)) {
                return m_creator.wrapElement(nextElem);
            }

        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_next != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public D next() {
        D next = m_next;
        m_next = getNext();
        return next;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<D> iterator() {
        return this;
    }

}
