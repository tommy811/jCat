package org.coderead.jcat.console

class ConsoleBase {
    def storage = [:]
    def propertyMissing(String name, value) { storage[name] = value }
    def propertyMissing(String name) { storage[name] }
}




