package Energos.Jenkins.OScript;

enum LockResourcesEnum{ 
    // lrUserSeanse("session"), 
    // lrBackgrowndWork("scheduledjobs") 
    // lrUserSeanse, 
    // lrBackgrowndWork 
    lrUserSeanse {
        @Override
        public String toString() {
            return "session";
        }
    },
    lrBackgrowndWork {
        @Override
        public String toString() {
            return "scheduledjobs";
        }
    }
}
