/*
 * Matcher.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package org.xhtmlrenderer.css.newmatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.css.constants.MarginBoxName;
import org.xhtmlrenderer.css.extend.AttributeResolver;
import org.xhtmlrenderer.css.extend.StylesheetFactory;
import org.xhtmlrenderer.css.extend.TreeResolver;
import org.xhtmlrenderer.css.sheet.FontFaceRule;
import org.xhtmlrenderer.css.sheet.MediaRule;
import org.xhtmlrenderer.css.sheet.PageRule;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.Ruleset;
import org.xhtmlrenderer.css.sheet.Stylesheet;
import org.xhtmlrenderer.util.Util;


/**
 * @author Torbjoern Gannholm
 */
public class Matcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Matcher.class);

    private final Mapper docMapper;
    private final org.xhtmlrenderer.css.extend.AttributeResolver _attRes;
    private final org.xhtmlrenderer.css.extend.TreeResolver _treeRes;
    private final org.xhtmlrenderer.css.extend.StylesheetFactory _styleFactory;

    private java.util.Map<Object, Mapper> _map;

    //handle dynamic
    private Set<Object> _hoverElements;
    private Set<Object> _activeElements;
    private Set<Object> _focusElements;
    private Set<Object> _visitElements;
    
    private final List<PageRule> _pageRules;
    private final List<FontFaceRule> _fontFaceRules;
    
    public Matcher(
            final TreeResolver tr, final AttributeResolver ar, final StylesheetFactory factory, final List<Stylesheet> stylesheets, final String medium) {
        newMaps();
        _treeRes = tr;
        _attRes = ar;
        _styleFactory = factory;
        
        _pageRules = new ArrayList<PageRule>();
        _fontFaceRules = new ArrayList<FontFaceRule>();
        docMapper = createDocumentMapper(stylesheets, medium);
    }
    
    public void removeStyle(final Object e) {
        _map.remove(e);
    }

    public CascadedStyle getCascadedStyle(final Object e, final boolean restyle) {
        synchronized (e) {
            Mapper em;
            if (!restyle) {
                em = getMapper(e);
            } else {
                em = matchElement(e);
            }
            return em.getCascadedStyle(e);
        }
    }

    /**
     * May return null.
     * We assume that restyle has already been done by a getCascadedStyle if necessary.
     */
    public CascadedStyle getPECascadedStyle(final Object e, final String pseudoElement) {
        synchronized (e) {
            final Mapper em = getMapper(e);
            return em.getPECascadedStyle(e, pseudoElement);
        }
    }
    
    public PageInfo getPageCascadedStyle(final String pageName, final String pseudoPage) 
    {
        final List<PropertyDeclaration> props = new ArrayList<>();
        final Map<MarginBoxName, List<PropertyDeclaration>> marginBoxes = new HashMap<>();

        _pageRules.stream()
                  .filter(pageRule -> pageRule.applies(pageName, pseudoPage))
                  .forEach(pageRule -> {
                      props.addAll(pageRule.getRuleset().getPropertyDeclarations());
                      marginBoxes.putAll(pageRule.getMarginBoxes());
                  });
        
        CascadedStyle style = null;
        if (props.isEmpty()) {
            style = CascadedStyle.emptyCascadedStyle;
        } else {
            style = new CascadedStyle(props.iterator());
        }
        
        return new PageInfo(props, style, marginBoxes);
    }
    
    public List<FontFaceRule> getFontFaceRules() {
        return _fontFaceRules;
    }
    
    public boolean isVisitedStyled(final Object e) {
        return _visitElements.contains(e);
    }

    public boolean isHoverStyled(final Object e) {
        return _hoverElements.contains(e);
    }

    public boolean isActiveStyled(final Object e) {
        return _activeElements.contains(e);
    }

    public boolean isFocusStyled(final Object e) {
        return _focusElements.contains(e);
    }

    protected Mapper matchElement(final Object e) {
        synchronized (e) {
            final Object parent = _treeRes.getParentElement(e);
            Mapper child;
            if (parent != null) {
                final Mapper m = getMapper(parent);
                child = m.mapChild(e);
            } else {//has to be document or fragment node
                child = docMapper.mapChild(e);
            }
            return child;
        }
    }

    Mapper createDocumentMapper(final List<Stylesheet> stylesheets, final String medium) {
        final java.util.TreeMap<String, Selector> sorter = new java.util.TreeMap<String, Selector>();
        addAllStylesheets(stylesheets, sorter, medium);
        LOGGER.info("Matcher created with " + sorter.size() + " selectors");
        return new Mapper(sorter.values());
    }
    
    private void addAllStylesheets(final List<Stylesheet> stylesheets, final TreeMap<String, Selector> sorter, final String medium) {
        int count = 0;
        int pCount = 0;
        for (final Stylesheet stylesheet : stylesheets) {
            for (final Object obj : stylesheet.getContents()) {
                if (obj instanceof Ruleset) {
                    for (final Selector selector : ((Ruleset)obj).getFSSelectors()) {
                        selector.setPos(++count);
                        sorter.put(selector.getOrder(), selector);
                    }
                } else if (obj instanceof PageRule) {
                    ((PageRule)obj).setPos(++pCount);
                    _pageRules.add((PageRule) obj);
                } else if (obj instanceof MediaRule) {
                    final MediaRule mediaRule = (MediaRule)obj;
                    if (mediaRule.matches(medium)) {
                        for (final Ruleset ruleset : mediaRule.getContents()) {
                            for (final Selector selector : ruleset.getFSSelectors()) {
                                selector.setPos(++count);
                                sorter.put(selector.getOrder(), selector);
                            }
                        }
                    }
                }
            }
            
            _fontFaceRules.addAll(stylesheet.getFontFaceRules());
        }
        
        Collections.sort(_pageRules, new Comparator<PageRule>() {
            public int compare(final PageRule o1, final PageRule o2) {
                final PageRule p1 = (PageRule)o1;
                final PageRule p2 = (PageRule)o2;
                
                if (p1.getOrder() - p2.getOrder() < 0) {
                    return -1;
                } else if (p1.getOrder() == p2.getOrder()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
    }

    private void link(final Object e, final Mapper m) {
        _map.put(e, m);
    }

    private void newMaps() {
        _map = Collections.synchronizedMap(new java.util.HashMap<Object, Mapper>());
        _hoverElements = Collections.synchronizedSet(new java.util.HashSet<Object>());
        _activeElements = Collections.synchronizedSet(new java.util.HashSet<Object>());
        _focusElements = Collections.synchronizedSet(new java.util.HashSet<Object>());
        _visitElements = Collections.synchronizedSet(new java.util.HashSet<Object>());
    }

    private Mapper getMapper(final Object e) {
        Mapper m = _map.get(e);
        if (m != null) {
            return m;
        }
        m = matchElement(e);
        return m;
    }

    private static java.util.Iterator<Ruleset> getMatchedRulesets(final List<Selector> mappedSelectors) {
        return
                new java.util.Iterator<Ruleset>() {
                    java.util.Iterator<Selector> selectors = mappedSelectors.iterator();

                    public boolean hasNext() {
                        return selectors.hasNext();
                    }

                    public Ruleset next() {
                        if (hasNext()) {
                            return selectors.next().getRuleset();
                        } else {
                            throw new java.util.NoSuchElementException();
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
    }

    private static java.util.Iterator<Ruleset> getSelectedRulesets(final java.util.List<Selector> selectorList) {
        final java.util.List<Selector> sl = selectorList;
        return
                new java.util.Iterator<Ruleset>() {
                    java.util.Iterator<Selector> selectors = sl.iterator();

                    public boolean hasNext() {
                        return selectors.hasNext();
                    }

                    public Ruleset next() {
                        if (hasNext()) {
                            return ((Selector) selectors.next()).getRuleset();
                        } else {
                            throw new java.util.NoSuchElementException();
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
    }

    private org.xhtmlrenderer.css.sheet.Ruleset getElementStyle(final Object e) {
        synchronized (e) {
            if (_attRes == null || _styleFactory == null) {
                return null;
            }
            
            final String style = _attRes.getElementStyling(e);
            if (Util.isNullOrEmpty(style)) {
                return null;
            }
            
            return _styleFactory.parseStyleDeclaration(org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin.AUTHOR, style);
        }
    }

    private org.xhtmlrenderer.css.sheet.Ruleset getNonCssStyle(final Object e) {
        synchronized (e) {
            if (_attRes == null || _styleFactory == null) {
                return null;
            }
            final String style = _attRes.getNonCssStyling(e);
            if (Util.isNullOrEmpty(style)) {
                return null;
            }
            return _styleFactory.parseStyleDeclaration(org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin.AUTHOR, style);
        }
    }

    /**
     * Mapper represents a local CSS for a Node that is used to match the Node's
     * children.
     *
     * @author Torbjoern Gannholm
     */
    class Mapper {
        java.util.List<Selector> axes;
        private HashMap<String, List<Selector>> pseudoSelectors;
        private List<Selector> mappedSelectors;
        private HashMap<String, Mapper> children;

        Mapper(final java.util.Collection<Selector> selectors) {
            axes = new java.util.ArrayList<Selector>(selectors.size());
            axes.addAll(selectors);
        }

        private Mapper() {
        }

        /**
         * Side effect: creates and stores a Mapper for the element
         *
         * @param e
         * @return The selectors that matched, sorted according to specificity
         *         (more correct: preserves the sort order from Matcher creation)
         */
        Mapper mapChild(final Object e) {
            //Mapper childMapper = new Mapper();
            final java.util.List<Selector> childAxes = new ArrayList<Selector>(axes.size() + 10);
            final java.util.HashMap<String, List<Selector>> pseudoSelectors = new java.util.HashMap<String, List<Selector>>();
            final java.util.List<Selector> mappedSelectors = new java.util.LinkedList<Selector>();
            final StringBuilder key = new StringBuilder();
            for (int i = 0, size = axes.size(); i < size; i++) {
                final Selector sel = axes.get(i);
                if (sel.getAxis() == Selector.DESCENDANT_AXIS) {
                    //carry it forward to other descendants
                    childAxes.add(sel);
                } else if (sel.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
                    throw new RuntimeException();
                }
                if (!sel.matches(e, _attRes, _treeRes)) {
                    continue;
                }
                //Assumption: if it is a pseudo-element, it does not also have dynamic pseudo-class
                final String pseudoElement = sel.getPseudoElement();
                if (pseudoElement != null) {
                    java.util.List<Selector> l = pseudoSelectors.get(pseudoElement);
                    if (l == null) {
                        l = new java.util.LinkedList<Selector>();
                        pseudoSelectors.put(pseudoElement, l);
                    }
                    l.add(sel);
                    key.append(sel.getSelectorID()).append(":");
                    continue;
                }
                if (sel.isPseudoClass(Selector.VISITED_PSEUDOCLASS)) {
                    _visitElements.add(e);
                }
                if (sel.isPseudoClass(Selector.ACTIVE_PSEUDOCLASS)) {
                    _activeElements.add(e);
                }
                if (sel.isPseudoClass(Selector.HOVER_PSEUDOCLASS)) {
                    _hoverElements.add(e);
                }
                if (sel.isPseudoClass(Selector.FOCUS_PSEUDOCLASS)) {
                    _focusElements.add(e);
                }
                if (!sel.matchesDynamic(e, _attRes, _treeRes)) {
                    continue;
                }
                key.append(sel.getSelectorID()).append(":");
                final Selector chain = sel.getChainedSelector();
                if (chain == null) {
                    mappedSelectors.add(sel);
                } else if (chain.getAxis() == Selector.IMMEDIATE_SIBLING_AXIS) {
                    throw new RuntimeException();
                } else {
                    childAxes.add(chain);
                }
            }
            if (children == null) children = new HashMap<String, Mapper>();
            Mapper childMapper = children.get(key.toString());
            if (childMapper == null) {
                childMapper = new Mapper();
                childMapper.axes = childAxes;
                childMapper.pseudoSelectors = pseudoSelectors;
                childMapper.mappedSelectors = mappedSelectors;
                children.put(key.toString(), childMapper);
            }
            link(e, childMapper);
            return childMapper;
        }

        CascadedStyle getCascadedStyle(final Object e) {
            CascadedStyle result;
            synchronized (e) {
                CascadedStyle cs = null;
                final org.xhtmlrenderer.css.sheet.Ruleset elementStyling = getElementStyle(e);
                final org.xhtmlrenderer.css.sheet.Ruleset nonCssStyling = getNonCssStyle(e);
                final List<PropertyDeclaration> propList = new LinkedList<PropertyDeclaration>();
                //specificity 0,0,0,0
                if (nonCssStyling != null) {
                    propList.addAll(nonCssStyling.getPropertyDeclarations());
                }
                //these should have been returned in order of specificity
                for (final Iterator<Ruleset> i = getMatchedRulesets(mappedSelectors); i.hasNext();) {
                    final org.xhtmlrenderer.css.sheet.Ruleset rs = (org.xhtmlrenderer.css.sheet.Ruleset) i.next();
                    propList.addAll(rs.getPropertyDeclarations());
                }
                //specificity 1,0,0,0
                if (elementStyling != null) {
                    propList.addAll(elementStyling.getPropertyDeclarations());
                }
                if (propList.size() == 0)
                    cs = CascadedStyle.emptyCascadedStyle;
                else {
                    cs = new CascadedStyle(propList.iterator());
                }

                result = cs;
            }
            return result;
        }

        /**
         * May return null.
         * We assume that restyle has already been done by a getCascadedStyle if necessary.
         */
        public CascadedStyle getPECascadedStyle(final Object e, final String pseudoElement) {
            final java.util.Iterator<Map.Entry<String, List<Selector>>> si = pseudoSelectors.entrySet().iterator();
            if (!si.hasNext()) {
                return null;
            }
            CascadedStyle cs = null;
            final java.util.List<Selector> pe = pseudoSelectors.get(pseudoElement);
            if (pe == null) return null;

            final java.util.List<PropertyDeclaration> propList = new java.util.LinkedList<PropertyDeclaration>();
            for (final java.util.Iterator<Ruleset> i = getSelectedRulesets(pe); i.hasNext();) {
                final org.xhtmlrenderer.css.sheet.Ruleset rs = (org.xhtmlrenderer.css.sheet.Ruleset) i.next();
                propList.addAll(rs.getPropertyDeclarations());
            }
            if (propList.size() == 0)
                cs = CascadedStyle.emptyCascadedStyle;//already internalized
            else {
                cs = new CascadedStyle(propList.iterator());
            }
            return cs;
        }
    }
}

