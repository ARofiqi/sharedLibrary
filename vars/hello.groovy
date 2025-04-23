def call() {
    "Hello call"
}

def world(){
    "hello world"
}

def say(String name){
    "hello ${name}"
}

def say(List<String> names){
    for (name in names) {
        echo "hello ${name}"
    }
}

def person(Map person){
    echo "hello ${person.name} and age ${person.age}"
}

def config(){
    def config = LibraryResource("config/build.json")
    def json = readJSON text: config
    echo "app: ${json.app}"
    echo "author: ${json.author}"
    echo "version: ${json.version}"
}