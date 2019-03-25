var chat = {}; // Namespace

(function () {
	"use strict";

	// Constants
	chat.ENTRY_ID = 'entry';
	chat.OUTPUT_ID = 'output';

	// Global variables
	chat.user = null;
	chat.ws = null

	chat.initPage = function () {
		document.getElementById(chat.ENTRY_ID).focus();

		chat.writeOutput('Initializing ChatServer client.');
		chat.writeOutput('Enter your username:');
	};

    chat.connect = function() {
        var url = new URL('/ws/' + encodeURI(chat.user), window.location.href);
        url.protocol = url.protocol.replace('http', 'ws');

        chat.ws = new WebSocket(url.href);
        chat.ws.onopen = function(evt) {
            chat.writeOutput('Connection established');
            chat.writeOutput('Type \'/help\' for a list of commands');
        };

        chat.ws.onclose = function(evt) {
            chat.writeOutput('Disconnected from server');
        };

        chat.ws.onmessage = function(evt) {
            // KeepAlive messages have no content
            if (evt.data !== '') {
                chat.writeOutput(evt.data);
            }
            else {
                console.debug('KeepAlive received');
            }
        };

        chat.ws.onerror = function(evt) {
            chat.writeOutput('There was a communications error, check the console for details');
            console.error("WebSocket Error", evt)
        }
    };

	chat.onEntryKeyPress = function (oCtl, oEvent) {
		if (chat.isEnterKeyPress(oEvent)) {
			// Capture the current text as a command
			var sEntry = oCtl.value.trim();

			// Reset the text entry for the next command
			oCtl.value = '';

			if (chat.user === null && chat.ws === null) {
				// Set the username first if we still need one
				if (sEntry.length > 0) {
					chat.user = sEntry;
                    chat.connect();
				}
			}
			else {
				// Process the entry
				if (sEntry !== '') {
    				chat.ws.send(sEntry);
                }
			}
		}
	};

	chat.isEnterKeyPress = function (oEvent) {
		var keynum;

		if (window.event) { // IE8 and earlier
			keynum = oEvent.keyCode;
		} else if (oEvent.which) { // IE9/Firefox/Chrome/Opera/Safari
			keynum = oEvent.which;
		}

		// Detect ENTER key
		return ('\n' === String.fromCharCode(keynum) || '\r' === String.fromCharCode(keynum));
	};

	chat.writeOutput = function (sOutput) {
		var oOutput, sPadding;
		oOutput = document.getElementById(chat.OUTPUT_ID);

		// Get a spacer unless we are the first entry
		sPadding = '\n';
		if (oOutput.value.length === 0) {
			sPadding = '';
		}

		// Append the output to the text area
		oOutput.value += sPadding + sOutput;

		// Scroll the text into view
		oOutput.scrollTop = oOutput.scrollHeight;
	};
}());
