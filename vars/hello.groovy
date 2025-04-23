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