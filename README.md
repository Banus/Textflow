# TextFlow: Screenless Access to Non-Visual Smart Messaging

This repository is the official implementation of the Android application
developed for the IUI 2021 paper "**_TextFlow_: Screenless Access to Non-Visual Smart Messaging**"
by Pegah Karimi, Emanuele Plebani and Davide Bolchini at the Indiana University
School of Informatics and Computing at IUPUI.

A short script to filter candidate sentences for the system (see Section 3.3 of
the paper) is in the `python` directory.

If you find this code useful, please cite the following paper:

```bibtex
@inproceedings{karimi2021Textflow,
  title={Textflow: Screenless Access to Non-Visual Smart Messaging},
  author={Karimi, Pegah and Plebani, Emanuele and Bolchini, Davide},
  affiliation={Indiana University-Purdue University Indianapolis}
  booktitle={Proceedings of the 26th International Conference on Intelligent User Interfaces},
  year={2021}
}
```

For more information on the content of this repository, contact the authors:
Pegah Karimi (pekarimi@iu.edu), Emanuele Plebani (eplebani@iu.edu) and Davide
Bolchini (dbolchin@iupui.edu).

## Overview

![System diagram](/images/system.png)

Textflow is a system implemented as an Android application to quickly generate
suggested messages in auditory form (via Text-To-Speech) from a set of selected
topics for blind and visual impaired (BVI) people.
The aural messages guide the user in the selection of topic and messages, and a
finger-worn device (TapStrap) connected via Bluetooth is used for the selection
itself.
A BVI user is thus able to compose a message without having to take the phone
in their hand.

## Requirements

You need Android Studio >=4.0 to generate the application.
You also need a [Tapstrap](https://www.tapwithus.com/) input device to select
the reccommendations.

For the Python script, you need to install `pytorch` and `fairseq`.
Install the former with conda:

```setup
conda install pytorch -c pytorch
```

For `faiseq`, follow the instructions on the
[official repository](https://github.com/pytorch/fairseq).

## Contributing

The code is released under the MIT license.
Bugfixes and contributions are welcome.

## Acknowledgments

This research is based on work supported by the National Science Foundatio
under Grant IIS #1909845 (PI: Davide Bolchini, Indiana University).
Any opinions, findings and conclusions or recommendations expressed in this
material are those of the authors and do not necessarily reflect those of the
NSF.
