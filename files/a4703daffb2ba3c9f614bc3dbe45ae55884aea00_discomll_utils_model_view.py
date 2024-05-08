import copy

from disco.core import result_iterator


def output_model(fitmodel_url):
    if "naivebayes_fitmodel" in fitmodel_url:
        output = _naivebayes_model(fitmodel_url["naivebayes_fitmodel"])
    elif "logreg_fitmodel" in fitmodel_url:
        output = _logreg_model(fitmodel_url["logreg_fitmodel"])
    elif "linsvm_fitmodel" in fitmodel_url:
        output = _linsvm_model(fitmodel_url["linsvm_fitmodel"])
    elif "kmeans_fitmodel" in fitmodel_url:
        output = _kmeans_model(fitmodel_url["kmeans_fitmodel"])
    elif "fddt_fitmodel" in fitmodel_url:
        output = _dt_model(fitmodel_url["fddt_fitmodel"])
    elif "drf_fitmodel" in fitmodel_url:
        output = _rf_model(fitmodel_url["drf_fitmodel"])
    elif "linreg_fitmodel" in fitmodel_url:
        output = _linreg_model(fitmodel_url["linreg_fitmodel"])
    else:
        output = "Model visualization not supported."
    return output


def _rf_model(fitmodel):
    X_names = []
    output = "Random forest model\n\n"
    for k, v in result_iterator(fitmodel):
        if k == "X_names":
            X_names = v
        elif k == "fill_in_values":
            continue
        else:
            for num, tree in enumerate(v):
                output += "tree" + str(num+1) + "\n"
                output += _tree_view(tree, X_names) + "\n"

    return output


def _dt_model(fitmodel):
    X_names = []
    output = "Decision Trees model\n\n"
    for k, v in result_iterator(fitmodel):
        if k == "X_names":
            X_names = v
        elif k == "fill_in_values":
            continue
        else:
            for num, tree in enumerate(v):
                output += "tree" + str(num+1) + "\n"
                output += _tree_view(tree, X_names) + "\n"

    return output


def _kmeans_model(fitmodel):
    output = "k-means model\n\n"
    output += "Centroids\n"
    for k, v in result_iterator(fitmodel):
        output += "Centroid " + str(k) + ": " + ", ".join(map(str, v["x"])) + "\n"
    return output


def _linsvm_model(fitmodel):
    output = "Linear SVM model\n\n"
    for k, v in result_iterator(fitmodel):
        if k == "params":
            output += "Parameters\n"
            output += ", ".join(map(str, v)) + "\n\n"
    return output


def _logreg_model(fitmodel):
    output = "Logistic regression model\n\n"
    for k, v in result_iterator(fitmodel):
        if k == "thetas":
            output += "Thetas\n"
            output += ", ".join(map(str, v)) + "\n\n"
        elif k == "J":
            output += "J cost function\n"
            output += str(v) + "\n\n"
    return output


def _linreg_model(fitmodel):
    output = "Linear regression model\n\n"
    for k, v in result_iterator(fitmodel):
        if k == "thetas":
            output += "Thetas\n"
            output += ", ".join(map(str, v)) + "\n\n"
    return output


def _naivebayes_model(fitmodel):
    output = "Naive Bayes model\n\n"
    for k, v in result_iterator(fitmodel):
        if k == "y_labels":
            output += "Classes\n"
            output += ", ".join(v) + "\n\n"
        elif k == "prior":
            output += "Prior probabilities\n"
            output += ", ".join(map(str, v)) + "\n\n"

        elif k == "mean":
            output += "Mean\n"
            output += ", ".join(map(str, v)) + "\n\n"
        elif k == "var":
            output += "Variance\n"
            output += ", ".join(map(str, v)) + "\n\n"

    return output


def _tree_view(tree, feature_names=[]):
    tree = copy.deepcopy(tree)
    output = ""
    stack = [0]

    if len(tree) > 0 and len(tree[0]) > 1:
        tree[0].pop(1)

    while len(stack) > 0:
        while stack[0] not in tree:
            stack.pop(0)
            if len(tree) == 0:
                return output
            else:
                del (tree[stack[0]][0])

        node = tree[stack[0]][0]
        spaces = "".join(["| " for i in range(node[4])])
        name = "atr" + str(node[1]) if feature_names == [] else feature_names[node[1]]

        if node[1] == -1:
            name, operator, split = "root", "", ""
        elif node[5] == "c":
            operator = " <= " if len(tree[stack[0]]) == 2 else " > "
            split = round(node[2], 4)
        else:
            operator = " in "
            split = "[" + ", ".join(sorted(node[2])) + "]"

        output += spaces + name + operator + str(split) + ", Dist: " + str(node[3]) + ", #Inst: " + str(
            sum(node[3].values())) + "\n"

        new_stack = [k[0] for k in tree[stack[0]]]
        if len(tree[stack[0]]) == 1:
            del (tree[stack[0]])
            stack.pop(0)
        stack = [new_stack[0]] + stack

    return output
