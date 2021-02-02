# proactive_BPM_adaptation

To recreate the experiments you can use docker and not have to install any of the requirements on your machine.

Simply download and setup docker, before running one of the 00_docker_builder scripts. The script will build two docker images for you, named "threshold-python" and "threshold-java". Within the root of this repository, execute the "docker-compose up" command, to run both images. 

Afterwards you can use "docker ps" to find the docker ids of the containers, which are now running your images. 

Then: 
1. Run "docker exec -it <id of the java container> sh" to open an interactive shell within the java-container. 
2. Execute "java -jar 00_ExperimentMain.jar" to load all the models into memory and set up four experiments with the four datasets. You can also give a list within swirly brackets (ex. {1,2,2,3}) to the jar in order to only create experiments with those specific datasets. Proceede to the next step, as soon as you see the output "Waiting for RL connection..."
3. Run "docker exec -it <id of the python container> bash" in a new terminal to open an interactive shell within the python-container.
4. Execute "python python_src/runexperiments.py" to start 4 experiments. You can tinker with the runexperiments.py script to modify those. 
5. The experiments will generate .csv files, numbered in the order of the experiments, within the python_src folder. Copy those into the python_src folder, then you can run "java -jar 00_GnuPlotter.jar" inside the java-container to plot pngs for all those generated csv files.
