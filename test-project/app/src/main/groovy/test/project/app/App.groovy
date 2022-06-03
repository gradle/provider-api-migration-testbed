package test.project.app

import org.apache.commons.text.WordUtils
import test.project.utilities.StringUtils

class App {
    static void main(String[] args) {
        def tokens = StringUtils.split(MessageUtils.message)
        def result = StringUtils.join(tokens)
        println(WordUtils.capitalize(result))
    }
}