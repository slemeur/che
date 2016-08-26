/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.part.explorer.project.macro;

import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;

import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

/**
 * Provider which is responsible for retrieving the resource name from the project explorer.
 * <p>
 * Macro provided: <code>${explorer.current.file.name}</code>
 * <p>
 * In case if project explorer has more than one selected file, comma separated file list is returned.
 *
 * @author Vlad Zhukovskyi
 * @see AbstractExplorerMacroProvider
 * @see ProjectExplorerPresenter
 * @since 4.7.0
 */
@Beta
@Singleton
public class ExplorerCurrentFileNameProvider extends AbstractExplorerMacroProvider {

    public static final String KEY = "${explorer.current.file.name}";

    private PromiseProvider promises;

    @Inject
    public ExplorerCurrentFileNameProvider(ProjectExplorerPresenter projectExplorer,
                                           PromiseProvider promises,
                                           AppContext appContext) {
        super(projectExplorer, appContext);
        this.promises = promises;
    }

    /** {@inheritDoc} */
    @Override
    public String getKey() {
        return KEY;
    }

    /** {@inheritDoc} */
    @Override
    public Promise<String> getValue() {

        List<Node> selectedNodes = getProjectExplorer().getTree().getSelectionModel().getSelectedNodes();

        if (selectedNodes.isEmpty()) {
            return promises.resolve("");
        }

        final Iterable<Resource> resources = transform(filter(selectedNodes, resNodePredicate), nodeToResourceFun);
        final String commaSeparated = Joiner.on(", ").join(transform(resources, resourceToNameFun));

        return promises.resolve(commaSeparated);
    }
}
