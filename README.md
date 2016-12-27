# Color-Things
RaspberryPI3, Android Things and Nearby Connections

LED Server to control a 12V RGB LED Strip.

Here is the "Color Connection" remote control app https://github.com/holgi-s/ColorConnection

## RaspberryPi3 in action:

![In action](ColorThings.jpg "in action")

## RaspberryPi3 Set-Up

![Fritzing](ColorThingsPi.png "fritzing")

Connect the power supply for the PWM board to 3.3V output of the PI.

&nbsp;

&nbsp;

&nbsp;

PS:

In my 1st test I connected the PWM board to the 5V output of the PI.
This was actually an error I made, because all the original PWM board examples I found were all aiming for the Arduino.

However, the documentation for the PCA9685 state that its power supply is valid from 2.3V to 5.5V and the IOs are 5V tolerant.
So this was no problem for the PWM board.

Because SDA and SCL of I²C bus are open-drain and the pull-ups for the bus are on the PI (3,3V) there was also no harm done for the PI.
