$.fn.space = function() {
  return this.type('space', 1);
};

$.fn.backspace = function() {
  return this.type('backspace', 1);
};

$.fn.type = function(str, nonCharacter, speed) {
  var keyCodes = {
   0: 48, 1: 49, 2: 50, 3: 51, 4: 52, 5: 53, 6: 54, 7: 55, 8: 56, 9: 57,
   a: 65, b: 66, c: 67, d: 68, e: 69, f: 70, g: 71, h: 72, i: 73, j: 74,
   k: 75, l: 76, m: 77, n: 78, o: 79, p: 80, q: 81, r: 82, s: 83, t: 84,
   u: 85, v: 86, w: 87, x: 88, y: 89, z: 90,
   '*': 106, '+': 107, '-': 109, '.': 110, ':': 111, ';': 186, '=': 187, ',': 188,
   '–': 189, '.': 190, '/': 191, '`': 192, '(': 219, '\\': 220, '(': 221, '\'': 222
  },
  nonCharacters = {
    'backspace': 8,
    'space': 32
  };     

  var triggerEventChain = function(elem, char, options) {
    // trigger keydown
    elem.simulate('keydown', options);
    var keydownPrevented = elem.data('typeevent').isDefaultPrevented();

    // if keydown's default action was prevented, also prevent it in keypress
    // (automatically done by browser if triggered by user)
    if (keydownPrevented)
      elem.bind('keypress.typeloop', function(e) { e.preventDefault(); });

    elem
      .simulate('keypress', options) // trigger keypress
      .unbind('.typeloop'); // unbind temporary event

    // for non gecko we need to actually modify the value
    // (somehow space as keyCode doesn't work programmatically in FF either)
    if ((!$.browser.mozilla || options.keyCode == 32) && !keydownPrevented) {
      if (char) {
        elem[0].value += char;
      } else {
        switch (options.keyCode) {
          case 32:
            elem[0].value += ' ';
            break;
          case 8:
            elem[0].value = elem[0].value.substr(0, elem[0].value.length-1);
            break;
        }
      }

    }

    // somehow space as keyCode doesn't work programmatically
    if(options.keyCode == 32) {
      elem[0].value += ' ';
    }       

    elem.simulate('keyup', options) // trigger keyup;
  };

  var queueEventChain = function() {
    var args = arguments;
    args[0].queue('type', function() {
        triggerEventChain.apply(this, args);
        window.setTimeout(function() { args[0].dequeue('type'); }, speed);
        });
  }

  return this.each(function() {
      var elem = $(this);

      // bind temporary event passing listeners       
      elem
      .bind('keydown.type', function(e) { $.data(this, 'typeevent', e); })
      .bind('keypress.type', function(e) { $.data(this, 'typeevent', e); })
      .bind('keyup.type', function(e) { $.data(this, 'typeevent', e); });

      if (nonCharacter) {

      } else {
        for (var i=0; i < str.length; i++) {
          var charCode = str.charCodeAt(i),
          upperCase = str[i] == str[i].toUpperCase();
          if (speed)
            queueEventChain(elem, str[i], { keyCode: keyCodes[str[i].toLowerCase()], charCode: charCode, shiftKey: upperCase });
          else
            triggerEventChain(elem, str[i], { keyCode: keyCodes[str[i].toLowerCase()], charCode: charCode, shiftKey: upperCase });
        };
      }

      elem.dequeue('type');

      // unbind temporary event passing listeners
      elem.unbind('.type');
  });
};

