var exec = require('cordova/exec');

var coolMethod = function () {};

coolMethod.readCard = function (success, error) {
    exec(success, error, 'UHF', 'readCard', []);
}

coolMethod.searchCard = function (success, error) {
    exec(success, error, 'UHF', 'searchCard', []);
}

coolMethod.writeCard = function (arg, success, error) {
    exec(success, error, 'UHF', 'writeCard', [arg]);
}

coolMethod.setPower = function (arg, success, error) {
    exec(success, error, 'UHF', 'setPower', [arg]);
}

coolMethod.startWork = function (success, error) {
    exec(success, error, 'UHF', 'startWork', []);
}

coolMethod.endWork = function (success, error) {
    exec(success, error, 'UHF', 'endWork', []);
}

coolMethod.selectCard = function (arg, success, error) {
    exec(success, error, 'UHF', 'selectCard', [arg]);
}

module.exports = coolMethod;
