#!/usr/bin/python3
# -*- coding: utf-8 -*-


class ObservableStore():
    def __init__(self, observables):
        self._observables = observables

    def getObservableElements(self):
        """
        get the list of properties that have observable decoration

        :return: list of observable properties.
        :rtype: Array
        """
        return self._observables

    def hasObservableElements(self):
        """
        Mention if class has observable element.

        :return: true if have observable element, otherwise false.
        :rtype: bool
        """
        return self._observables.__len__() > 0

    def areObservableElements(self, elementNames):
        """
        Mention if all elements are observable element.

        :param str ElementName: the element name to evaluate
        :return: true if is an observable element, otherwise false.
        :rtype: bool
        """
        if not(hasattr(elementNames, "__len__")):
            raise TypeError(
                "Element name should be a array of strings." +
                "I receive this {0}"
                .format(elementNames))

        return self._evaluateArray(elementNames)

    def isObservableElement(self, elementName):
        """
        Mention if an element is an observable element.

        :param str ElementName: the element name to evaluate
        :return: true if is an observable element, otherwise false.
        :rtype: bool
        """
        if not(isinstance(elementName, str)):
            raise TypeError(
                "Element name should be a string ." +
                "I receive this {0}"
                .format(elementName))

        return (True if (elementName == "*")
                else self._evaluateString(elementName))

    def _evaluateString(self, elementNames):
        result = False
        if (elementNames in self._observables):
            result = True
        return result

    def _evaluateArray(self, elementNames):
        result = False
        if set(elementNames).issubset(self._observables):
            result = True
        return result

    def add(self, observableElement):
        """
        add an observable element

        :param str observableElement: the name of the observable element
        :raises RuntimeError: if element name already exist in the store
        """
        if observableElement not in self._observables:
            self._observables.append(observableElement)
        else:
            raise RuntimeError(
                "{0} is already an observable element"
                .format(observableElement))

    def remove(self, observableElement):
        """
        remove an obsrvable element

        :param str observableElement: the name of the observable element
        """
        if observableElement in self._observables:
            self._observables.remove(observableElement)
